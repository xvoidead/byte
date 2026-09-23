package dev.byteide.runner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.springframework.stereotype.Service;

/**
 * Компилирует один исходный файл с помощью встроенного javac и находит класс с методом main.
 */
@Service
public class JavaCompilationService {

    static final String DEFAULT_CLASS_NAME = "Main";

    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "(?m)^public\\s+(?:(?:final|abstract|sealed|non-sealed|strictfp)\\s+)*(?:class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)");

    private final JavaCompiler compiler;
    private final List<String> options;

    public JavaCompilationService() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("javac недоступен: сервер нужно запускать на JDK, а не на JRE");
        }
        this.options = List.of(
                "-proc:none",
                "-encoding", "UTF-8",
                "-g",
                "-Xmaxerrs", "50",
                "--release", String.valueOf(Runtime.version().feature()));
    }

    public CompilationResult compile(String source) {
        long start = System.nanoTime();
        String fileClassName = publicTypeName(source).orElse(DEFAULT_CLASS_NAME);
        Path classesDir = createTempDir();
        boolean keepDir = false;
        try {
            DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
            boolean ok;
            try (StandardJavaFileManager fileManager =
                         compiler.getStandardFileManager(collector, Locale.ROOT, StandardCharsets.UTF_8)) {
                fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classesDir.toFile()));
                JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, collector, options, null,
                        List.of(new StringSource(fileClassName, source)));
                ok = task.call();
            }

            List<Diagnostic> diagnostics = new ArrayList<>();
            for (var d : collector.getDiagnostics()) {
                toDiagnostic(d, source).ifPresent(diagnostics::add);
            }

            if (!ok) {
                return new CompilationResult(null, diagnostics, elapsedMs(start));
            }

            Optional<String> mainClass = findMainClass(classesDir, fileClassName);
            if (mainClass.isEmpty()) {
                diagnostics.add(new Diagnostic(Diagnostic.Severity.ERROR, 0, 0, 0,
                        "Не найден метод public static void main(String[] args)",
                        "Программа начинает выполнение с метода main. Добавьте его в класс Main."));
                return new CompilationResult(null, diagnostics, elapsedMs(start));
            }

            keepDir = true;
            return new CompilationResult(new CompiledProgram(classesDir, mainClass.get()), diagnostics, elapsedMs(start));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!keepDir) {
                new CompiledProgram(classesDir, "").close();
            }
        }
    }

    static Optional<String> publicTypeName(String source) {
        Matcher matcher = PUBLIC_TYPE.matcher(source);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static Optional<Diagnostic> toDiagnostic(javax.tools.Diagnostic<? extends JavaFileObject> d, String source) {
        Diagnostic.Severity severity = switch (d.getKind()) {
            case ERROR -> Diagnostic.Severity.ERROR;
            case WARNING, MANDATORY_WARNING -> Diagnostic.Severity.WARNING;
            default -> null;
        };
        if (severity == null) {
            return Optional.empty();
        }
        long line = Math.max(0, d.getLineNumber());
        long column = Math.max(0, d.getColumnNumber());
        long endColumn = column;
        if (column > 0 && d.getPosition() >= 0) {
            long length = Math.max(1, d.getEndPosition() - d.getPosition());
            long lineEnd = source.indexOf('\n', (int) d.getPosition());
            if (lineEnd < 0) {
                lineEnd = source.length();
            }
            endColumn = column + Math.max(1, Math.min(length, lineEnd - d.getPosition()));
        }
        return Optional.of(new Diagnostic(severity, line, column, endColumn,
                d.getMessage(Locale.ROOT), CompilerHints.forCode(d.getCode())));
    }

    /**
     * Ищет класс с public static void main(String[]). Классы загружаются без инициализации,
     * поэтому пользовательский код (статические блоки) на сервере не выполняется.
     */
    private static Optional<String> findMainClass(Path classesDir, String preferred) throws IOException {
        List<String> classNames;
        try (Stream<Path> files = Files.walk(classesDir)) {
            classNames = files
                    .filter(p -> p.toString().endsWith(".class"))
                    .map(p -> classesDir.relativize(p).toString())
                    .map(p -> p.substring(0, p.length() - ".class".length()).replace('/', '.').replace('\\', '.'))
                    .sorted()
                    .toList();
        }
        if (classNames.contains(preferred) && hasMain(classesDir, preferred)) {
            return Optional.of(preferred);
        }
        return classNames.stream().filter(name -> hasMain(classesDir, name)).findFirst();
    }

    private static boolean hasMain(Path classesDir, String className) {
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Class<?> type = Class.forName(className, false, loader);
            Method main = type.getDeclaredMethod("main", String[].class);
            int mods = main.getModifiers();
            return Modifier.isStatic(mods) && Modifier.isPublic(mods) && main.getReturnType() == void.class;
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("byte-run-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static final class StringSource extends SimpleJavaFileObject {
        private final String code;

        StringSource(String className, String code) {
            super(URI.create("string:///" + className + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
