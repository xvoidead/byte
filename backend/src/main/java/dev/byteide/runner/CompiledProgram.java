package dev.byteide.runner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Скомпилированные классы во временном каталоге. Каталог удаляется при закрытии.
 *
 * @param heavy программа запускает PostgreSQL внутри себя: ей нужно больше памяти и времени
 *              (см. {@link RunnerProperties#heavyMemoryMb()})
 */
public record CompiledProgram(Path classesDir, String mainClass, boolean heavy) implements AutoCloseable {

    public CompiledProgram(Path classesDir, String mainClass) {
        this(classesDir, mainClass, false);
    }

    @Override
    public void close() {
        try (Stream<Path> files = Files.walk(classesDir)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
