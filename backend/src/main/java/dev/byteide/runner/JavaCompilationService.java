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

import com.sun.source.util.JavacTask;

import org.springframework.stereotype.Service;

/**
 * Компилирует проект ученика встроенным javac (с библиотеками из {@link StudentLibraries} в classpath),
 * кладёт ресурсы рядом с классами и находит класс с методом main.
 */
@Service
public class JavaCompilationService {

    static final String DEFAULT_CLASS_NAME = "Main";

    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "(?m)^public\\s+(?:(?:final|abstract|sealed|non-sealed|strictfp)\\s+)*(?:class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)");

    /** javac иногда сдаётся без сообщения — например, на очень глубокой вложенности скобок. */
    static final Diagnostic UNREADABLE = Diagnostic.general(
            "Компилятор не смог разобрать программу",
            "Скорее всего, в коде слишком глубокая вложенность скобок или выражений. Упростите код.",
            "byte.unreadable");

    private final JavaCompiler compiler;
    private final List<String> options;
    private final List<Path> libraries;

    public JavaCompilationService() {
        this(StudentLibraries.shared());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JavaCompilationService(StudentLibraries libraries) {
        this.libraries = libraries.jars();
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
        return compile(Project.single(source));
    }

    public CompilationResult compile(Project project) {
        long start = System.nanoTime();
        List<StringSource> sources = sources(project);
        Path classesDir = createTempDir();
        boolean keepDir = false;
        try {
            DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
            boolean ok;
            try (StandardJavaFileManager fileManager =
                         compiler.getStandardFileManager(collector, Locale.ROOT, StandardCharsets.UTF_8)) {
                fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classesDir.toFile()));
                fileManager.setLocation(StandardLocation.CLASS_PATH, libraries.stream().map(Path::toFile).toList());
                JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, collector, options, null, sources);
                ok = callSafely(task);
            }

            List<Diagnostic> diagnostics = diagnostics(collector);
            if (!ok) {
                if (diagnostics.stream().noneMatch(d -> d.severity() == Diagnostic.Severity.ERROR)) {
                    diagnostics.add(UNREADABLE);
                }
                return new CompilationResult(null, diagnostics, elapsedMs(start));
            }

            Optional<String> mainClass = findMainClass(classesDir, preferredMainClass(sources));
            if (mainClass.isEmpty()) {
                diagnostics.add(Diagnostic.general(
                        "Не найден метод public static void main(String[] args)",
                        "Программа начинает выполнение с метода main. Добавьте его в класс Main.", "byte.no.main"));
                return new CompilationResult(null, diagnostics, elapsedMs(start));
            }
            copyResources(project, classesDir);

            keepDir = true;
            return new CompilationResult(new CompiledProgram(classesDir, mainClass.get(), project.usesPostgres()),
                    diagnostics, elapsedMs(start));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!keepDir) {
                new CompiledProgram(classesDir, "").close();
            }
        }
    }

    /**
     * Проверка кода на ошибки без генерации классов и без запуска — для подсветки ошибок во время набора.
     * javac разбирает код, проверяет типы и поток выполнения, но ничего не пишет на диск.
     */
    public DiagnosticsResult diagnose(String source) {
        return diagnose(Project.single(source));
    }

    public DiagnosticsResult diagnose(Project project) {
        long start = System.nanoTime();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        boolean ok;
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(collector, Locale.ROOT, StandardCharsets.UTF_8)) {
            fileManager.setLocation(StandardLocation.CLASS_PATH, libraries.stream().map(Path::toFile).toList());
            JavacTask task = (JavacTask) compiler.getTask(null, fileManager, collector, options, null, sources(project));
            ok = analyzeSafely(task);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<Diagnostic> diagnostics = diagnostics(collector);
        if (!ok && diagnostics.stream().noneMatch(d -> d.severity() == Diagnostic.Severity.ERROR)) {
            diagnostics.add(UNREADABLE);
        }
        return new DiagnosticsResult(diagnostics, elapsedMs(start));
    }

    public record DiagnosticsResult(List<Diagnostic> diagnostics, long timeMs) {
    }

    private static boolean analyzeSafely(JavacTask task) {
        try {
            task.analyze();
            return true;
        } catch (StackOverflowError | IllegalStateException e) {
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof StackOverflowError) {
                return false;
            }
            throw e;
        }
    }

    /** На патологическом коде javac может переполнить стек — это ошибка кода ученика, а не сервера. */
    private static boolean callSafely(JavaCompiler.CompilationTask task) {
        try {
            return task.call();
        } catch (StackOverflowError | IllegalStateException e) {
            return false;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof StackOverflowError) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Исходники для javac. javac требует, чтобы публичный класс лежал в файле с тем же именем. Если файл один,
     * имя берётся из самого класса — так ученик может переименовать класс, не думая об имени файла.
     * В проекте из нескольких файлов действует обычное правило Java.
     */
    private static List<StringSource> sources(Project project) {
        List<ProjectFile> files = project.sources();
        if (files.size() == 1) {
            ProjectFile file = files.getFirst();
            String className = publicTypeName(file.content()).orElse(DEFAULT_CLASS_NAME);
            return List.of(new StringSource(className + ".java", file.name(), file.content()));
        }
        return files.stream().map(f -> new StringSource(f.name(), f.name(), f.content())).toList();
    }

    /** Если main есть в нескольких классах, запускается Main (или публичный класс единственного файла). */
    private static String preferredMainClass(List<StringSource> sources) {
        if (sources.size() == 1) {
            String uri = sources.getFirst().toUri().getPath();
            return uri.substring(uri.lastIndexOf('/') + 1, uri.length() - ".java".length());
        }
        return DEFAULT_CLASS_NAME;
    }

    /** Файлы resources/... кладутся рядом с классами — их можно прочитать через getResourceAsStream. */
    private static void copyResources(Project project, Path classesDir) throws IOException {
        for (ProjectFile file : project.resources()) {
            Path target = classesDir.resolve(file.name().substring(Project.RESOURCES.length())).normalize();
            if (!target.startsWith(classesDir)) {
                continue;
            }
            Files.createDirectories(target.getParent());
            Files.writeString(target, file.content(), StandardCharsets.UTF_8);
        }
    }

    private static List<Diagnostic> diagnostics(DiagnosticCollector<JavaFileObject> collector) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (var d : collector.getDiagnostics()) {
            toDiagnostic(d).ifPresent(diagnostics::add);
        }
        return diagnostics;
    }

    static Optional<String> publicTypeName(String source) {
        Matcher matcher = PUBLIC_TYPE.matcher(source);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static Optional<Diagnostic> toDiagnostic(javax.tools.Diagnostic<? extends JavaFileObject> d) {
        Diagnostic.Severity severity = switch (d.getKind()) {
            case ERROR -> Diagnostic.Severity.ERROR;
            case WARNING, MANDATORY_WARNING -> Diagnostic.Severity.WARNING;
            default -> null;
        };
        if (severity == null) {
            return Optional.empty();
        }
        String file = d.getSource() instanceof StringSource source ? source.fileName : null;
        String source = d.getSource() instanceof StringSource src ? src.code : "";
        long line = Math.max(0, d.getLineNumber());
        long column = Math.max(0, d.getColumnNumber());
        long endColumn = column;
        if (column > 0 && d.getPosition() >= 0 && d.getPosition() <= source.length()) {
            long length = Math.max(1, d.getEndPosition() - d.getPosition());
            long lineEnd = source.indexOf('\n', (int) d.getPosition());
            if (lineEnd < 0) {
                lineEnd = source.length();
            }
            endColumn = column + Math.max(1, Math.min(length, lineEnd - d.getPosition()));
        }
        return Optional.of(new Diagnostic(severity, line, column, endColumn,
                d.getMessage(Locale.ROOT), CompilerHints.forCode(d.getCode()), d.getCode(), file));
    }

    /**
     * Ищет класс с public static void main(String[]). Классы загружаются без инициализации,
     * поэтому пользовательский код (статические блоки) на сервере не выполняется.
     */
    private Optional<String> findMainClass(Path classesDir, String preferred) throws IOException {
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

    private boolean hasMain(Path classesDir, String className) {
        List<URL> urls = new ArrayList<>();
        try {
            urls.add(classesDir.toUri().toURL());
            for (Path jar : libraries) {
                urls.add(jar.toUri().toURL());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader())) {
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
        private final String fileName;
        private final String code;

        /**
         * @param path     путь для javac: по нему проверяется, что публичный класс лежит в файле со своим именем
         * @param fileName имя файла в проекте — его видит ученик в сообщениях об ошибках
         */
        StringSource(String path, String fileName, String code) {
            super(URI.create("string:///" + path), Kind.SOURCE);
            this.fileName = fileName;
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
