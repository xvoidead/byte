package dev.byteide.runner;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Самопроверка песочницы: запускает программу, которая пытается сделать запрещённое.
 * Пока проверка не пройдена, код учеников не выполняется. Если защита не работает
 * (например, JVM новее Java 23, где SecurityManager удалён), сервер отказывается запускать программы.
 */
@Component
public class SandboxHealth {

    private static final Logger log = LoggerFactory.getLogger(SandboxHealth.class);

    static final String PROBE = """
            public class Main {
                static void probe(String name, Runnable action) {
                    try {
                        action.run();
                        System.out.println("OPEN " + name);
                    } catch (SecurityException e) {
                        System.out.println("BLOCKED " + name);
                    }
                }

                public static void main(String[] args) {
                    probe("env", () -> System.getenv("PATH"));
                    probe("process", () -> ProcessHandle.current().parent());
                    probe("file", () -> new java.io.File("/").list());
                    probe("exec", () -> {
                        try {
                            new ProcessBuilder("true").start();
                        } catch (java.io.IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    probe("network", () -> {
                        try {
                            new java.net.Socket("127.0.0.1", 9).close();
                        } catch (java.io.IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    System.out.println("Привет");
                }
            }
            """;

    static final List<String> EXPECTED = List.of(
            "BLOCKED env", "BLOCKED process", "BLOCKED file", "BLOCKED exec", "BLOCKED network", "Привет");

    private final JavaCompilationService compiler;
    private final ProcessExecutionService executor;
    private volatile Boolean verified;
    private volatile String problem;
    /** Не synchronized: внутри ждём процесс, а это закрепило бы виртуальный поток за носителем. */
    private final ReentrantLock lock = new ReentrantLock();

    public SandboxHealth(JavaCompilationService compiler, ProcessExecutionService executor) {
        this.compiler = compiler;
        this.executor = executor;
    }

    @EventListener(ApplicationReadyEvent.class)
    void verifyOnStartup() {
        Thread.ofVirtual().start(this::ensureVerified);
    }

    /** Бросает исключение, если песочница не работает. Первая проверка выполняется один раз. */
    public void ensureVerified() {
        Boolean ok = verified;
        if (ok == null) {
            ok = verify();
        }
        if (!ok) {
            throw new SandboxUnavailableException(problem);
        }
    }

    /** null — ещё не проверялась. */
    public Boolean status() {
        return verified;
    }

    private boolean verify() {
        lock.lock();
        try {
            if (verified != null) {
                return verified;
            }
            runProbe();
        } finally {
            lock.unlock();
        }
        return verified;
    }

    private void runProbe() {
        try {
            CompilationResult compilation = compiler.compile(PROBE);
            if (!compilation.success()) {
                problem = "Проверочная программа не скомпилировалась: " + compilation.diagnostics();
                verified = false;
            } else {
                try (CompiledProgram program = compilation.program()) {
                    ExecutionResult result = executor.execute(program, "");
                    List<String> lines = result.stdout().lines().toList();
                    verified = result.status() == RunStatus.SUCCESS && lines.equals(EXPECTED);
                    if (!verified) {
                        problem = "Защита песочницы не сработала: " + lines + " " + result.stderr().strip();
                    }
                }
            }
        } catch (RuntimeException e) {
            problem = "Песочница не запускается: " + e.getMessage();
            verified = false;
        }
        if (verified) {
            log.info("Песочница проверена: запрещённые операции блокируются");
        } else {
            log.error("Песочница не работает, запуск программ выключен. {}", problem);
        }
    }
}
