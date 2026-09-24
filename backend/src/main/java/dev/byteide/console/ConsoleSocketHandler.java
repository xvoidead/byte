package dev.byteide.console;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import dev.byteide.runner.CodeRunner;
import dev.byteide.runner.Diagnostic;
import dev.byteide.runner.ExecutionResult;
import dev.byteide.runner.RunnerBusyException;
import dev.byteide.runner.RunnerProperties;
import dev.byteide.runner.RunningProgram;
import dev.byteide.runner.SandboxUnavailableException;
import dev.byteide.web.ClientIp;
import dev.byteide.web.RateLimiter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Интерактивная консоль: программа запускается, её вывод приходит по мере появления,
 * а ученик вводит данные прямо в консоли, как в настоящей IDE.
 *
 * <p>Сообщения клиента: {@code run {code}}, {@code input {data}}, {@code eof}, {@code stop}.
 * Сообщения сервера: {@code compile_error}, {@code started}, {@code out {stream, data}}, {@code exit}, {@code error}.
 */
@Component
public class ConsoleSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSocketHandler.class);

    static final String IP_ATTRIBUTE = "byte.clientIp";

    private final CodeRunner runner;
    private final RateLimiter limiter;
    private final RunnerProperties properties;
    private final JsonMapper json;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public ConsoleSocketHandler(CodeRunner runner, RateLimiter limiter, RunnerProperties properties, JsonMapper json) {
        this.runner = runner;
        this.limiter = limiter;
        this.properties = properties;
        this.json = json;
    }

    /** Состояние одного подключения: не больше одной программы одновременно. */
    private final class Session {
        final WebSocketSession socket;
        final String ip;
        volatile RunningProgram program;
        final AtomicInteger inputChars = new AtomicInteger();

        Session(WebSocketSession socket, String ip) {
            this.socket = new ConcurrentWebSocketSessionDecorator(socket, 10_000, 512 * 1024);
            this.ip = ip;
        }

        void send(Map<String, Object> message) {
            try {
                if (socket.isOpen()) {
                    socket.sendMessage(new TextMessage(json.writeValueAsString(message)));
                }
            } catch (IOException | RuntimeException e) {
                stopProgram();
            }
        }

        void stopProgram() {
            RunningProgram running = program;
            if (running != null) {
                running.stop();
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession socket) {
        // Код программы до 50 000 символов в UTF-8 — с запасом.
        socket.setTextMessageSizeLimit(256 * 1024);
        Object ip = socket.getAttributes().get(IP_ATTRIBUTE);
        sessions.put(socket.getId(), new Session(socket, ip == null ? "unknown" : ip.toString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession socket, CloseStatus status) {
        Session session = sessions.remove(socket.getId());
        if (session != null) {
            session.stopProgram();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession socket, TextMessage message) {
        Session session = sessions.get(socket.getId());
        if (session == null) {
            return;
        }
        JsonNode node;
        try {
            node = json.readTree(message.getPayload());
        } catch (RuntimeException e) {
            session.send(error("Некорректное сообщение."));
            return;
        }
        String type = node.path("type").asString("");
        switch (type) {
            case "run" -> run(session, node.path("code").asString(""));
            case "input" -> input(session, node.path("data").asString(""));
            case "eof" -> {
                RunningProgram running = session.program;
                if (running != null) {
                    running.closeInput();
                }
            }
            case "stop" -> session.stopProgram();
            default -> session.send(error("Неизвестное сообщение."));
        }
    }

    private void run(Session session, String code) {
        if (code.isBlank()) {
            session.send(error("Код программы пуст."));
            return;
        }
        if (code.length() > properties.maxSourceLength()) {
            session.send(error("Код слишком длинный: максимум " + properties.maxSourceLength() + " символов."));
            return;
        }
        session.stopProgram();
        RateLimiter.Decision decision = limiter.tryAcquire(session.ip, RateLimiter.Category.RUN);
        if (!decision.allowed()) {
            session.send(error("Слишком много запусков подряд. Подождите " + decision.retryAfterSeconds() + " с."));
            return;
        }
        if (!limiter.acquireRun(session.ip)) {
            session.send(error("Дождитесь завершения программ, запущенных в других вкладках."));
            return;
        }
        boolean handedOver = false;
        try {
            session.inputChars.set(0);
            CodeRunner.Interactive result = runner.startInteractive(code, new RunningProgram.Listener() {
                @Override
                public void onOutput(RunningProgram.Stream stream, String text) {
                    session.send(Map.of("type", "out",
                            "stream", stream == RunningProgram.Stream.STDOUT ? "stdout" : "stderr", "data", text));
                }

                @Override
                public void onExit(ExecutionResult execution) {
                    limiter.releaseRun(session.ip);
                    session.program = null;
                    Map<String, Object> exit = new LinkedHashMap<>();
                    exit.put("type", "exit");
                    exit.put("status", execution.status());
                    exit.put("exitCode", execution.exitCode());
                    exit.put("timeMs", execution.timeMs());
                    session.send(exit);
                    log.info("console status={} run={}ms ip={}", execution.status(), execution.timeMs(),
                            ClientIp.masked(session.ip));
                }
            });
            switch (result) {
                case CodeRunner.CompilationFailed failed -> session.send(Map.of("type", "compile_error",
                        "diagnostics", failed.compilation().diagnostics(),
                        "compileTimeMs", failed.compilation().timeMs()));
                case CodeRunner.Started started -> {
                    handedOver = true;
                    session.program = started.program();
                    session.send(Map.of("type", "started",
                            "diagnostics", warnings(started.compilation().diagnostics()),
                            "compileTimeMs", started.compilation().timeMs()));
                }
            }
        } catch (RunnerBusyException e) {
            session.send(error(e.getMessage()));
        } catch (SandboxUnavailableException e) {
            session.send(error("Сервер временно не может запускать программы. Попробуйте позже."));
        } catch (RuntimeException e) {
            log.warn("Не удалось запустить программу в консоли", e);
            session.send(error("Не удалось запустить программу."));
        } finally {
            if (!handedOver) {
                limiter.releaseRun(session.ip);
            }
        }
    }

    private void input(Session session, String data) {
        RunningProgram running = session.program;
        if (running == null || data.isEmpty()) {
            return;
        }
        if (session.inputChars.addAndGet(data.length()) > properties.maxStdinLength()) {
            session.send(error("Слишком много ввода для одной программы."));
            running.closeInput();
            return;
        }
        running.write(data);
    }

    private static List<Diagnostic> warnings(List<Diagnostic> diagnostics) {
        return diagnostics.stream().filter(d -> d.severity() == Diagnostic.Severity.WARNING).toList();
    }

    private static Map<String, Object> error(String message) {
        return Map.of("type", "error", "message", message);
    }
}
