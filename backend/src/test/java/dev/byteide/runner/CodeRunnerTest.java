package dev.byteide.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class CodeRunnerTest {

    static final RunnerProperties PROPERTIES = new RunnerProperties(Duration.ofSeconds(3), Duration.ofSeconds(3),
            Duration.ofSeconds(20), 64, 16, 1024, 50_000, 100_000, 2, 2, 2, Duration.ofSeconds(10), 2048, 4096, 100, null);
    static final JavaCompilationService COMPILER = new JavaCompilationService();
    static final ProcessExecutionService EXECUTOR = new ProcessExecutionService(PROPERTIES, new Sandbox(PROPERTIES));
    static final SandboxHealth HEALTH = new SandboxHealth(COMPILER, EXECUTOR);

    private final CodeRunner runner = new CodeRunner(COMPILER, EXECUTOR, HEALTH, PROPERTIES);

    @Test
    void runsProgramAndCapturesUnicodeOutput() {
        RunResult result = runner.run("""
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Привет, мир!");
                    }
                }
                """, "");

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("Привет, мир!\n");
        assertThat(result.stderr()).isEmpty();
        assertThat(result.exitCode()).isZero();
    }

    @Test
    void passesStdinToProgram() {
        RunResult result = runner.run("""
                import java.util.Scanner;
                public class Main {
                    public static void main(String[] args) {
                        Scanner in = new Scanner(System.in);
                        String name = in.nextLine();
                        int a = in.nextInt(), b = in.nextInt();
                        System.out.println(name + ": " + (a + b));
                    }
                }
                """, "Сумма\n2 40\n");

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("Сумма: 42\n");
    }

    @Test
    void reportsCompilationErrorsWithPositionAndHint() {
        RunResult result = runner.run("""
                public class Main {
                    public static void main(String[] args) {
                        int x = 5
                        System.out.println(y);
                    }
                }
                """, "");

        assertThat(result.status()).isEqualTo(RunStatus.COMPILATION_ERROR);
        assertThat(result.diagnostics()).isNotEmpty();
        Diagnostic first = result.diagnostics().getFirst();
        assertThat(first.severity()).isEqualTo(Diagnostic.Severity.ERROR);
        assertThat(first.line()).isEqualTo(3);
        assertThat(first.message()).contains("';' expected");
        assertThat(first.hint()).contains("точка с запятой");
    }

    @Test
    void reportsUnknownSymbol() {
        RunResult result = runner.run("""
                public class Main {
                    public static void main(String[] args) {
                        System.out.println(counter);
                    }
                }
                """, "");

        assertThat(result.status()).isEqualTo(RunStatus.COMPILATION_ERROR);
        Diagnostic d = result.diagnostics().getFirst();
        assertThat(d.line()).isEqualTo(3);
        assertThat(d.column()).isEqualTo(28);
        assertThat(d.endColumn()).isEqualTo(35);
        assertThat(d.hint()).contains("Имя не найдено");
    }

    @Test
    void reportsRuntimeException() {
        RunResult result = runner.run("""
                public class Main {
                    public static void main(String[] args) {
                        int[] a = new int[2];
                        System.out.println("до");
                        a[5] = 1;
                    }
                }
                """, "");

        assertThat(result.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(result.stdout()).isEqualTo("до\n");
        assertThat(result.stderr()).contains("ArrayIndexOutOfBoundsException");
        assertThat(result.exitCode()).isNotZero();
    }

    @Test
    void stopsInfiniteLoop() {
        RunResult result = runner.run("""
                public class Main {
                    public static void main(String[] args) {
                        while (true) { }
                    }
                }
                """, "");

        assertThat(result.status()).isEqualTo(RunStatus.TIMEOUT);
    }

    @Test
    void stopsProgramThatWaitsForInputForever() {
        RunResult result = runner.run("""
                public class Main {
                    public static void main(String[] args) throws Exception {
                        Thread.sleep(60_000);
                    }
                }
                """, "");

        assertThat(result.status()).isEqualTo(RunStatus.TIMEOUT);
        assertThat(result.runTimeMs()).isLessThan(10_000);
    }

    @Test
    void limitsOutputSize() {
        RunResult result = runner.run("""
                public class Main {
                    public static void main(String[] args) {
                        while (true) System.out.println("спам");
                    }
                }
                """, "");

        assertThat(result.status()).isEqualTo(RunStatus.OUTPUT_LIMIT);
        assertThat(result.stdout().length()).isLessThanOrEqualTo(1024);
    }

    @Test
    void findsMainInClassWithAnotherName() {
        RunResult result = runner.run("""
                class Helper {
                    static String text() { return "ok"; }
                }

                public class Hello {
                    public static void main(String[] args) {
                        System.out.println(Helper.text());
                    }
                }
                """, "");

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("ok\n");
    }

    @Test
    void findsMainInNonPublicClass() {
        RunResult result = runner.run("""
                class Program {
                    public static void main(String[] args) {
                        System.out.println("works");
                    }
                }
                """, "");

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("works\n");
    }

    @Test
    void reportsMissingMain() {
        RunResult result = runner.run("""
                public class Main {
                    static void hello() { }
                }
                """, "");

        assertThat(result.status()).isEqualTo(RunStatus.COMPILATION_ERROR);
        assertThat(result.diagnostics()).anySatisfy(d -> assertThat(d.message()).contains("main"));
    }

    @Test
    void doesNotRunStaticInitializersOnServer() {
        RunResult result = runner.run("""
                public class Main {
                    static {
                        try {
                            System.setProperty("byte.test.marker", "set");
                        } catch (SecurityException e) {
                            // в песочнице запрещено
                        }
                    }
                    public static void main(String[] args) {
                        System.out.println("ok");
                    }
                }
                """, "");

        assertThat(result.stdout()).isEqualTo("ok\n");
        assertThat(System.getProperty("byte.test.marker")).isNull();
    }

    @Test
    void detectsPublicTypeName() {
        assertThat(JavaCompilationService.publicTypeName("public final class Foo {}")).contains("Foo");
        assertThat(JavaCompilationService.publicTypeName("class A {}\npublic record Point(int x) {}")).contains("Point");
        assertThat(JavaCompilationService.publicTypeName("class A {}")).isEmpty();
    }
}
