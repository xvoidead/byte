package dev.byteide.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Атаки из ревью безопасности: всё это должно блокироваться песочницей. */
class SandboxSecurityTest {

    /** Стек-трейсы запретов длинные, поэтому лимит вывода здесь обычный, а не тестовый в 1024 символа. */
    private static final RunnerProperties PROPERTIES = RunnerProperties.defaults();
    private static final ProcessExecutionService EXECUTOR =
            new ProcessExecutionService(PROPERTIES, new Sandbox(PROPERTIES));

    private final CodeRunner runner = new CodeRunner(CodeRunnerTest.COMPILER, EXECUTOR,
            new SandboxHealth(CodeRunnerTest.COMPILER, EXECUTOR), PROPERTIES);

    private RunResult run(String body) {
        return runner.run("""
                import java.io.*;
                import java.net.*;
                import java.nio.file.*;
                import java.util.*;

                public class Main {
                    public static void main(String[] args) throws Exception {
                        %s
                    }
                }
                """.formatted(body), "");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ProcessHandle.current().parent().ifPresent(ProcessHandle::destroyForcibly);",
            "System.out.println(System.getenv());",
            "System.out.println(System.getenv(\"PATH\"));",
            "System.out.println(Files.readAllLines(Path.of(\"/etc/passwd\")));",
            "System.out.println(Files.readString(Path.of(\"/proc/self/environ\")));",
            "Files.writeString(Path.of(\"/tmp/byte-escape\"), \"x\");",
            "new ProcessBuilder(\"sh\", \"-c\", \"sleep 100 &\").start();",
            "Runtime.getRuntime().exec(new String[]{\"id\"});",
            "new Socket(\"127.0.0.1\", 8080).close();",
            "new URI(\"http://example.com\").toURL().openStream();",
            "System.out.println(System.getProperty(\"user.home\"));",
            "var f = String.class.getDeclaredField(\"value\"); f.setAccessible(true);",
            "System.setOut(new PrintStream(new ByteArrayOutputStream()));",
            "System.setSecurityManager(null);",
            // Рабочая папка не выпускает наружу.
            "System.out.println(Files.readString(Path.of(\"../sandbox.policy\")));",
            "System.out.println(Arrays.toString(new File(\"..\").list()));",
            "Files.createSymbolicLink(Path.of(\"link\"), Path.of(\"/etc/passwd\"));",
            "Files.createLink(Path.of(\"hard\"), Path.of(\"/etc/hostname\"));",
            // Защита не доступна через рефлексию, хотя рефлексия внутри своего кода разрешена.
            "var f = System.getSecurityManager().getClass().getDeclaredFields()[0]; f.setAccessible(true);",
            "java.lang.invoke.MethodHandles.privateLookupIn(System.getSecurityManager().getClass(), java.lang.invoke.MethodHandles.lookup());",
            "var m = Thread.class.getDeclaredMethod(\"setPriority0\", int.class); m.setAccessible(true);",
            "Class.forName(\"sun.misc.Unsafe\");",
            "Class.forName(\"jdk.internal.misc.Unsafe\");",
            "new URLClassLoader(new URL[0]);",
    })
    void blocksDangerousOperations(String body) {
        RunResult result = run(body);

        assertThat(result.status()).as(body).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).as(body).containsAnyOf("AccessControlException", "SecurityException",
                "InaccessibleObjectException", "IllegalAccessException", "ClassNotFoundException");
        assertThat(new java.io.File("/tmp/byte-escape")).doesNotExist();
    }

    @Test
    void allowsOrdinaryProgramsAndHidesSandboxWarnings() {
        RunResult result = run("""
                List<Integer> list = new ArrayList<>(List.of(3, 1, 2));
                Collections.sort(list);
                Thread t = new Thread(() -> System.out.println("поток"));
                t.start();
                t.join();
                System.out.println(list + " " + String.format(Locale.US, "%.1f", 2.5)
                        + " " + java.util.stream.IntStream.rangeClosed(1, 100).parallel().sum()
                        + " " + System.lineSeparator().length());
                """);

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("поток\n[1, 2, 3] 2.5 5050 1\n");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void limitsNumberOfThreads() {
        RunResult result = run("""
                for (int i = 0; i < 1000; i++) {
                    new Thread(() -> {
                        try { Thread.sleep(10_000); } catch (InterruptedException e) { }
                    }).start();
                }
                """);

        assertThat(result.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("Слишком много потоков");
    }

    @Test
    void stopsProgramThatBurnsCpuInManyThreads() {
        // Лимит процессорного времени учитывает все потоки и срабатывает задолго до лимита по часам,
        // сколько бы ядер ни было у машины.
        RunnerProperties defaults = RunnerProperties.defaults();
        RunnerProperties properties = new RunnerProperties(Duration.ofSeconds(10), Duration.ofSeconds(1),
                defaults.interactiveTimeout(), defaults.maxMemoryMb(), defaults.maxThreads(), defaults.maxOutputChars(),
                defaults.maxSourceLength(), defaults.maxStdinLength(), defaults.maxConcurrentRuns(),
                defaults.maxInteractiveSessions(), defaults.maxConcurrentCompiles(), defaults.queueTimeout(),
                defaults.maxFileSizeKb(), defaults.maxWorkDirSizeKb(), defaults.maxWorkDirEntries(), null);
        ProcessExecutionService executor = new ProcessExecutionService(properties, new Sandbox(properties));
        CodeRunner cpuLimited = new CodeRunner(CodeRunnerTest.COMPILER, executor,
                new SandboxHealth(CodeRunnerTest.COMPILER, executor), properties);

        RunResult result = cpuLimited.run("""
                public class Main {
                    public static void main(String[] args) {
                        for (int i = 0; i < 8; i++) {
                            new Thread(() -> { while (true) { } }).start();
                        }
                    }
                }
                """, "");

        assertThat(result.status()).isEqualTo(RunStatus.TIMEOUT);
        assertThat(result.runTimeMs()).isLessThan(7_000);
    }

    @Test
    void allowsSystemExit() {
        RunResult result = run("System.out.println(\"до\"); System.exit(3);");

        assertThat(result.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.stdout()).isEqualTo("до\n");
    }

    @Test
    void printsStackTraceWithoutSandboxFramesAndCollapsesRecursion() {
        RunResult result = runner.run("""
                public class Main {
                    static int depth(int n) {
                        return depth(n + 1) + 1;
                    }
                    public static void main(String[] args) {
                        depth(0);
                    }
                }
                """, "");

        assertThat(result.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        List<String> lines = result.stderr().lines().toList();
        assertThat(lines.getFirst()).isEqualTo("Exception in thread \"main\" java.lang.StackOverflowError");
        assertThat(result.stderr()).contains("at Main.depth(Main.java:3)").contains("таких же вызовов");
        assertThat(result.stderr()).doesNotContain("dev.byteide").doesNotContain("reflect");
        assertThat(lines).hasSizeLessThan(10);
    }

    @Test
    void reportsUnreadableCodeInsteadOfEmptyError() {
        // Глубина с запасом: разогретый JIT-ом javac тратит на вызов меньше стека.
        String nested = "(".repeat(20_000) + "1" + ")".repeat(20_000);
        RunResult result = runner.run(
                "public class Main { public static void main(String[] a) { int x = " + nested + "; } }", "");

        assertThat(result.status()).isEqualTo(RunStatus.COMPILATION_ERROR);
        assertThat(result.diagnostics()).isNotEmpty();
        assertThat(result.diagnostics()).allSatisfy(d -> assertThat(d.message()).isNotBlank());
    }
}
