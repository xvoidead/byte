package dev.byteide.runner;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Библиотеки, которыми могут пользоваться программы учеников: Gson, SnakeYAML, JDBC-драйверы SQLite и PostgreSQL,
 * драйвер MongoDB, Paper API и MockBukkit. Maven кладёт их в {@code classpath:sandbox-lib/}, а мосты к ним
 * (src/bridge) — в {@code classpath:sandbox-bridge/}. При старте всё копируется во временный каталог, потому что
 * javac и дочерней JVM нужны обычные файлы, а не записи внутри jar сервера.
 *
 * <p>Каталог мостов стоит в classpath первым: его классы заменяют одноимённые классы библиотек
 * (например, {@code MongoClients} подключается к MongoDB в памяти, а не по сети).
 */
public final class StudentLibraries {

    static final String BRIDGE_DIR = "bridge";

    private static volatile StudentLibraries shared;

    private final Path root;
    private final List<Path> jars;

    private StudentLibraries(Path root, List<Path> jars) {
        this.root = root;
        this.jars = List.copyOf(jars);
    }

    /** Один набор на процесс: копируется при первом обращении и удаляется при завершении JVM. */
    public static StudentLibraries shared() {
        StudentLibraries result = shared;
        if (result == null) {
            synchronized (StudentLibraries.class) {
                result = shared;
                if (result == null) {
                    result = load();
                    shared = result;
                }
            }
        }
        return result;
    }

    /** Classpath программ: каталог мостов, затем jar-файлы библиотек по алфавиту. */
    public List<Path> jars() {
        return jars;
    }

    /** Каталог, в котором лежат все библиотеки и мосты. */
    public Path root() {
        return root;
    }

    private static StudentLibraries load() {
        try {
            Path dir = Files.createTempDirectory("byte-libs-");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteQuietly(dir)));
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            List<Path> jars = new ArrayList<>();
            for (Resource resource : resolver.getResources("classpath*:sandbox-lib/*.jar")) {
                Path target = dir.resolve(resource.getFilename());
                copy(resource, target);
                jars.add(target);
            }
            jars.sort(Comparator.naturalOrder());

            Path bridge = dir.resolve(BRIDGE_DIR);
            int copied = 0;
            for (Resource resource : resolver.getResources("classpath*:sandbox-bridge/**")) {
                String url = resource.getURL().toString();
                String name = url.substring(url.lastIndexOf("sandbox-bridge/") + "sandbox-bridge/".length());
                if (name.isEmpty() || name.endsWith("/") || !resource.isReadable()) {
                    continue;
                }
                Path target = bridge.resolve(name).normalize();
                if (!target.startsWith(bridge)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                copy(resource, target);
                copied++;
            }
            if (copied > 0) {
                jars.addFirst(bridge);
            }
            return new StudentLibraries(dir, jars);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось подготовить библиотеки для программ", e);
        }
    }

    private static void copy(Resource resource, Path target) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, target);
        }
    }

    private static void deleteQuietly(Path dir) {
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
            // временный каталог — не страшно
        }
    }
}
