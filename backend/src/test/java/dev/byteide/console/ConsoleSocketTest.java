package dev.byteide.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsoleSocketTest {

    @LocalServerPort
    int port;

    @Autowired
    JsonMapper json;

    private final BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();

    private WebSocketSession connect(String origin) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setOrigin(origin);
        return new StandardWebSocketClient().execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                received.add(json.readTree(message.getPayload()));
            }
        }, headers, URI.create("ws://localhost:" + port + "/api/console")).get(5, TimeUnit.SECONDS);
    }

    private void send(WebSocketSession session, Map<String, Object> message) throws Exception {
        session.sendMessage(new TextMessage(json.writeValueAsString(message)));
    }

    private JsonNode next(String type) throws InterruptedException {
        while (true) {
            JsonNode node = received.poll(15, TimeUnit.SECONDS);
            assertThat(node).as("ждём сообщение " + type).isNotNull();
            if (node.path("type").asString().equals(type)) {
                return node;
            }
        }
    }

    /** Собирает вывод до сообщения exit. */
    private String outputUntilExit(StringBuilder out) throws InterruptedException {
        while (true) {
            JsonNode node = received.poll(15, TimeUnit.SECONDS);
            assertThat(node).isNotNull();
            switch (node.path("type").asString()) {
                case "out" -> out.append(node.path("data").asString());
                case "exit" -> {
                    return node.path("status").asString();
                }
                default -> {
                }
            }
        }
    }

    @Test
    void programAsksForInputAndGetsItWhileRunning() throws Exception {
        WebSocketSession session = connect("http://localhost:" + port);
        send(session, Map.of("type", "run", "code", """
                import java.util.Scanner;
                public class Main {
                    public static void main(String[] args) {
                        Scanner in = new Scanner(System.in);
                        System.out.print("Как тебя зовут? ");
                        String name = in.nextLine();
                        System.out.println("Привет, " + name + "!");
                    }
                }
                """));
        next("started");
        StringBuilder out = new StringBuilder();
        while (!out.toString().contains("Как тебя зовут?")) {
            JsonNode node = next("out");
            out.append(node.path("data").asString());
        }
        send(session, Map.of("type", "input", "data", "Ада\n"));
        String status = outputUntilExit(out);

        assertThat(status).isEqualTo("SUCCESS");
        assertThat(out.toString()).isEqualTo("Как тебя зовут? Привет, Ада!\n");
        session.close();
    }

    @Test
    void stopsProgramOnRequest() throws Exception {
        WebSocketSession session = connect("http://localhost:" + port);
        send(session, Map.of("type", "run", "code", """
                public class Main {
                    public static void main(String[] args) throws Exception {
                        new java.util.Scanner(System.in).nextLine();
                    }
                }
                """));
        next("started");
        send(session, Map.of("type", "stop"));
        JsonNode exit = next("exit");

        assertThat(exit.path("status").asString()).isEqualTo("STOPPED");
        session.close();
    }

    @Test
    void reportsCompilationErrors() throws Exception {
        WebSocketSession session = connect("http://localhost:" + port);
        send(session, Map.of("type", "run", "code", "public class Main { void x( }"));
        JsonNode error = next("compile_error");

        assertThat(error.path("diagnostics").size()).isPositive();
        session.close();
    }

    @Test
    void acceptsLargePrograms() throws Exception {
        WebSocketSession session = connect("http://localhost:" + port);
        String comment = "// " + "комментарий ".repeat(3500) + "\n";
        send(session, Map.of("type", "run", "code",
                comment + "public class Main { public static void main(String[] a) { System.out.println(42); } }"));
        next("started");
        StringBuilder out = new StringBuilder();

        assertThat(outputUntilExit(out)).isEqualTo("SUCCESS");
        assertThat(out.toString()).isEqualTo("42\n");
        session.close();
    }

    @Test
    void rejectsForeignOrigins() {
        assertThatThrownBy(() -> connect("https://evil.example")).isNotNull();
        assertThat(List.of()).isEmpty();
    }
}
