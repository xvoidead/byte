package dev.byteide.lessons;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CodeInspectorTest {

    private final CodeInspector inspector = new CodeInspector();

    private static Requirement req(String type, String name) {
        return new Requirement(type, name, null, null, null, null, null, null, null, null, "m");
    }

    private boolean check(String source, Requirement requirement) {
        return inspector.check(source, List.of(requirement)).getFirst().passed();
    }

    @Test
    void detectsLoopsAndRecursion() {
        String loop = "class A { void f() { for (int i = 0; i < 3; i++) { } } }";
        String recursion = "class A { static int sum(int n) { return n == 0 ? 0 : n + sum(n - 1); } }";

        assertThat(check(loop, req("loop", null))).isTrue();
        assertThat(check(recursion, req("loop", null))).isFalse();
        assertThat(check(recursion, req("noLoops", null))).isTrue();
        assertThat(check(recursion, req("recursion", "sum"))).isTrue();
        assertThat(check(loop, req("recursion", "f"))).isFalse();
    }

    @Test
    void checksMethodSignatureAndCalls() {
        String source = """
                import java.util.Arrays;
                class Main {
                    static boolean isPrime(int n) { return n > 1; }
                    public static void main(String[] a) { int[] x = {3, 1}; Arrays.sort(x); isPrime(5); }
                }
                """;
        Requirement signature = new Requirement("method", "isPrime", "boolean", List.of("int"),
                null, null, null, null, null, null, "m");
        Requirement wrongSignature = new Requirement("method", "isPrime", "int", null,
                null, null, null, null, null, null, "m");

        assertThat(check(source, signature)).isTrue();
        assertThat(check(source, wrongSignature)).isFalse();
        assertThat(check(source, req("call", "isPrime"))).isTrue();
        assertThat(check(source, req("forbidCall", "Arrays.sort"))).isFalse();
        assertThat(check(source, req("forbidCall", "sort"))).isFalse();
        assertThat(check(source, req("forbidCall", "Collections.sort"))).isTrue();
    }

    @Test
    void forbidsHardcodedAnswersEvenInsideStrings() {
        Requirement noAnswers = new Requirement("forbidLiteral", null, null, null, List.of("21"),
                null, null, null, null, null, "m");

        assertThat(check("class A { int x = 21; }", noAnswers)).isFalse();
        assertThat(check("class A { String s = \"Площадь: 21\"; }", noAnswers)).isFalse();
        assertThat(check("class A { String s = \"Площадь: 210\"; int w = 7 * 3; }", noAnswers)).isTrue();
    }

    @Test
    void checksClassesAndFields() {
        String source = """
                interface Shape { double area(); }
                abstract class Base implements Shape { }
                class Circle extends Base { private double r; public double area() { return r; } }
                record Point(int x, int y) { }
                """;
        Requirement circle = new Requirement("class", "Circle", null, null, null, "Base", null, null, null, null, "m");
        Requirement base = new Requirement("class", "Base", null, null, null, null, "Shape", null, null, null, "m");
        Requirement point = new Requirement("class", "Point", null, null, null, null, null, "record", null, null, "m");
        Requirement field = new Requirement("privateField", "r", null, null, null, null, null, null, "Circle", null, "m");

        assertThat(check(source, circle)).isTrue();
        assertThat(check(source, base)).isTrue();
        assertThat(check(source, point)).isTrue();
        assertThat(check(source, field)).isTrue();
    }
}
