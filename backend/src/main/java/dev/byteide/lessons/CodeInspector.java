package dev.byteide.lessons;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.springframework.stereotype.Component;

import dev.byteide.runner.Project;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;

/** Проверяет требования к устройству решения по синтаксическому дереву (без компиляции в байт-код). */
@Component
public class CodeInspector {

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    public record Outcome(String message, boolean passed) {
    }

    public List<Outcome> check(String source, List<Requirement> requirements) {
        return check(Project.single(source), requirements);
    }

    /** Требования проверяются по всем исходникам проекта вместе. */
    public List<Outcome> check(Project project, List<Requirement> requirements) {
        if (requirements.isEmpty()) {
            return List.of();
        }
        Facts facts = inspect(project);
        return requirements.stream().map(r -> new Outcome(r.message(), facts.satisfies(r))).toList();
    }

    Facts inspect(String source) {
        return inspect(Project.single(source));
    }

    Facts inspect(Project project) {
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            List<Source> sources = project.sources().stream().map(f -> new Source(f.name(), f.content())).toList();
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostic -> {
            }, List.of("-proc:none"), null, sources);
            Facts facts = new Facts();
            for (CompilationUnitTree unit : task.parse()) {
                new Collector(facts).scan(unit, null);
            }
            return facts;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (StackOverflowError e) {
            return new Facts();
        }
    }

    /** Что есть в программе. */
    static final class Facts {
        int loops;
        int lambdas;
        int tries;
        int throwsCount;
        int switches;
        final List<MethodTree> methods = new ArrayList<>();
        final List<String> calls = new ArrayList<>();
        final Set<String> literals = new HashSet<>();
        final List<String> strings = new ArrayList<>();
        final List<ClassTree> classes = new ArrayList<>();
        final Set<String> recursive = new HashSet<>();
        final List<String> instantiated = new ArrayList<>();
        /** Поле: "Класс.имя" → приватное ли. */
        final List<String[]> fields = new ArrayList<>();

        boolean satisfies(Requirement r) {
            return switch (r.type()) {
                case "loop" -> loops > 0;
                case "noLoops" -> loops == 0;
                case "method" -> methods.stream().anyMatch(m -> matchesMethod(m, r));
                case "call" -> calls.stream().anyMatch(c -> callMatches(c, r.name()));
                case "forbidCall" -> names(r).stream().noneMatch(name -> calls.stream().anyMatch(c -> callMatches(c, name)));
                case "forbidLiteral" -> r.values().stream().noneMatch(this::containsLiteral);
                case "literal" -> names(r).stream().allMatch(this::containsText);
                case "recursion" -> recursive.contains(r.name());
                case "class" -> classes.stream().anyMatch(c -> matchesClass(c, r));
                case "privateField" -> fields.stream()
                        .anyMatch(f -> f[0].equals(r.className()) && f[1].equals(r.name()) && f[2].equals("private"));
                case "uses" -> uses(r);
                case "avoid" -> !uses(r);
                default -> throw new IllegalArgumentException("Неизвестный тип требования: " + r.type());
            };
        }

        private boolean uses(Requirement r) {
            return switch (r.construct()) {
                case "lambda" -> lambdas > 0;
                case "try" -> tries > 0;
                case "throw" -> throwsCount > 0;
                case "switch" -> switches > 0;
                case "stream" -> calls.stream().anyMatch(c -> callMatches(c, "stream") || callMatches(c, "lines"));
                case "new" -> instantiated.contains(r.name());
                default -> throw new IllegalArgumentException("Неизвестная конструкция: " + r.construct());
            };
        }

        private static List<String> names(Requirement r) {
            return r.name() != null ? List.of(r.name()) : r.values();
        }

        /** Строка (обычно SQL-запрос) содержит фрагмент — без учёта регистра и лишних пробелов. */
        private boolean containsText(String fragment) {
            String expected = squeeze(fragment);
            return strings.stream().anyMatch(s -> squeeze(s).contains(expected));
        }

        private static String squeeze(String text) {
            return text.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
        }

        /** Число запрещено и как литерал, и внутри строки ("Площадь: 21"), если оно не часть другого числа. */
        private boolean containsLiteral(String value) {
            if (literals.contains(value)) {
                return true;
            }
            java.util.regex.Pattern standalone = java.util.regex.Pattern.compile(
                    "(?<![\\d.])" + java.util.regex.Pattern.quote(value) + "(?![\\d])");
            return strings.stream().anyMatch(s -> standalone.matcher(s).find());
        }

        private static boolean matchesMethod(MethodTree m, Requirement r) {
            if (!m.getName().contentEquals(r.name())) {
                return false;
            }
            if (r.returns() != null && (m.getReturnType() == null || !m.getReturnType().toString().equals(r.returns()))) {
                return false;
            }
            if (r.params() != null) {
                List<String> types = m.getParameters().stream().map(p -> p.getType().toString()).toList();
                return types.equals(r.params());
            }
            return true;
        }

        private static boolean matchesClass(ClassTree c, Requirement r) {
            if (!c.getSimpleName().contentEquals(r.name())) {
                return false;
            }
            if (r.kind() != null && !c.getKind().name().equalsIgnoreCase(r.kind())) {
                return false;
            }
            if (r.superclass() != null
                    && (c.getExtendsClause() == null || !baseName(c.getExtendsClause()).equals(r.superclass()))) {
                return false;
            }
            if (r.interfaceName() != null) {
                List<? extends Tree> interfaces = c.getKind() == Tree.Kind.INTERFACE
                        ? c.getExtendsClause() == null ? List.of() : List.of(c.getExtendsClause())
                        : c.getImplementsClause();
                return interfaces.stream().anyMatch(i -> baseName(i).equals(r.interfaceName()));
            }
            return true;
        }

        private static String baseName(Tree type) {
            String text = type.toString();
            int lt = text.indexOf('<');
            return lt < 0 ? text : text.substring(0, lt);
        }

        /** "Arrays.sort" совпадает с вызовом Arrays.sort(...) и java.util.Arrays.sort(...); "sort" — с любым sort. */
        private static boolean callMatches(String call, String expected) {
            return call.equals(expected) || call.endsWith("." + expected)
                    || (!expected.contains(".") && call.substring(call.lastIndexOf('.') + 1).equals(expected));
        }
    }

    private static final class Collector extends TreeScanner<Void, Void> {
        private final Facts facts;
        private final Deque<String> methodStack = new ArrayDeque<>();
        private final Deque<String> classStack = new ArrayDeque<>();

        Collector(Facts facts) {
            this.facts = facts;
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
            facts.classes.add(node);
            classStack.push(node.getSimpleName().toString());
            try {
                return super.visitClass(node, unused);
            } finally {
                classStack.pop();
            }
        }

        @Override
        public Void visitMethod(MethodTree node, Void unused) {
            facts.methods.add(node);
            methodStack.push(node.getName().toString());
            try {
                return super.visitMethod(node, unused);
            } finally {
                methodStack.pop();
            }
        }

        @Override
        public Void visitVariable(VariableTree node, Void unused) {
            // Поле класса: объявлено прямо в теле класса, а не в методе.
            if (methodStack.isEmpty() && !classStack.isEmpty()) {
                boolean isPrivate = node.getModifiers().getFlags().contains(Modifier.PRIVATE);
                facts.fields.add(new String[]{classStack.peek(), node.getName().toString(), isPrivate ? "private" : "open"});
            }
            return super.visitVariable(node, unused);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
            String name = switch (node.getMethodSelect()) {
                case MemberSelectTree select -> select.getExpression() + "." + select.getIdentifier();
                case IdentifierTree ident -> ident.getName().toString();
                default -> node.getMethodSelect().toString();
            };
            facts.calls.add(name);
            String simple = name.substring(name.lastIndexOf('.') + 1);
            boolean selfCall = node.getMethodSelect() instanceof IdentifierTree
                    || (node.getMethodSelect() instanceof MemberSelectTree select
                    && Objects.equals(select.getExpression().toString(), "this"));
            if (selfCall && !methodStack.isEmpty() && methodStack.peek().equals(simple)) {
                facts.recursive.add(simple);
            }
            return super.visitMethodInvocation(node, unused);
        }

        @Override
        public Void visitMemberReference(MemberReferenceTree node, Void unused) {
            facts.lambdas++;
            facts.calls.add(node.getQualifierExpression() + "." + node.getName());
            return super.visitMemberReference(node, unused);
        }

        @Override
        public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
            facts.lambdas++;
            return super.visitLambdaExpression(node, unused);
        }

        @Override
        public Void visitNewClass(NewClassTree node, Void unused) {
            String type = node.getIdentifier().toString();
            int lt = type.indexOf('<');
            facts.instantiated.add(lt < 0 ? type : type.substring(0, lt));
            return super.visitNewClass(node, unused);
        }

        @Override
        public Void visitLiteral(LiteralTree node, Void unused) {
            if (node.getValue() instanceof String text) {
                facts.strings.add(text);
            } else {
                facts.literals.add(String.valueOf(node.getValue()));
            }
            return super.visitLiteral(node, unused);
        }

        @Override
        public Void visitUnary(UnaryTree node, Void unused) {
            // -5 — это унарный минус и литерал 5; запоминаем и "-5".
            if (node.getKind() == Tree.Kind.UNARY_MINUS && node.getExpression() instanceof LiteralTree literal) {
                facts.literals.add("-" + literal.getValue());
            }
            return super.visitUnary(node, unused);
        }

        @Override
        public Void visitForLoop(ForLoopTree node, Void unused) {
            facts.loops++;
            return super.visitForLoop(node, unused);
        }

        @Override
        public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
            facts.loops++;
            return super.visitEnhancedForLoop(node, unused);
        }

        @Override
        public Void visitWhileLoop(WhileLoopTree node, Void unused) {
            facts.loops++;
            return super.visitWhileLoop(node, unused);
        }

        @Override
        public Void visitDoWhileLoop(DoWhileLoopTree node, Void unused) {
            facts.loops++;
            return super.visitDoWhileLoop(node, unused);
        }

        @Override
        public Void visitTry(TryTree node, Void unused) {
            facts.tries++;
            return super.visitTry(node, unused);
        }

        @Override
        public Void visitThrow(ThrowTree node, Void unused) {
            facts.throwsCount++;
            return super.visitThrow(node, unused);
        }

        @Override
        public Void visitSwitch(SwitchTree node, Void unused) {
            facts.switches++;
            return super.visitSwitch(node, unused);
        }

        @Override
        public Void visitSwitchExpression(SwitchExpressionTree node, Void unused) {
            facts.switches++;
            return super.visitSwitchExpression(node, unused);
        }
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String code;

        Source(String name, String code) {
            super(URI.create("string:///" + name), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
