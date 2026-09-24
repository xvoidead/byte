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
 * Библиотеки, которыми могут пользоваться программы учеников (Gson, SnakeYAML).
 * Maven кладёт их в {@code classpath:sandbox-lib/}; при старте они копируются во временный каталог,
 * потому что javac и дочерней JVM нужны обычные файлы, а не записи внутри jar сервера.
 */
public final class StudentLibraries {

    private static volatile StudentLibraries shared;

    private final List<Path> jars;

    private StudentLibraries(List<Path> jars) {
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

    public List<Path> jars() {
        return jars;
    }

    private static StudentLibraries load() {
        try {
            Path dir = Files.createTempDirectory("byte-libs-");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteQuietly(dir)));
            List<Path> jars = new ArrayList<>();
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:sandbox-lib/*.jar");
            for (Resource resource : resources) {
                Path target = dir.resolve(resource.getFilename());
                try (InputStream in = resource.getInputStream()) {
                    Files.copy(in, target);
                }
                jars.add(target);
            }
            jars.sort(Comparator.naturalOrder());
            return new StudentLibraries(jars);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось подготовить библиотеки для программ", e);
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
