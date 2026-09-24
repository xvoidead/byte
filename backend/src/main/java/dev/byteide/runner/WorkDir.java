package dev.byteide.runner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Рабочая папка программы: единственное место, где песочница разрешает читать и писать файлы.
 * Создаётся на каждый запуск с файлами проекта и удаляется после него.
 */
public final class WorkDir implements AutoCloseable {

    /** Файлы крупнее не показываем ученику целиком. */
    static final int MAX_TEXT_BYTES = 64 * 1024;
    /** Сколько файлов возвращаем после запуска. */
    static final int MAX_RETURNED_FILES = Project.MAX_FILES;

    private final Path path;

    private WorkDir(Path path) {
        this.path = path;
    }

    public static WorkDir create(List<ProjectFile> files) {
        try {
            Path dir = Files.createTempDirectory("byte-work-");
            for (ProjectFile file : files) {
                Path target = dir.resolve(file.name()).normalize();
                if (!target.startsWith(dir)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                Files.writeString(target, file.content(), StandardCharsets.UTF_8);
            }
            return new WorkDir(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось подготовить рабочую папку", e);
        }
    }

    public Path path() {
        return path;
    }

    /** Размер и число записей; останавливает подсчёт, как только превышен любой из лимитов. */
    Usage usage(long maxBytes, int maxEntries) {
        long[] bytes = {0};
        int[] entries = {0};
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return dir.equals(path) ? FileVisitResult.CONTINUE : count(0);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    return count(attrs.size());
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    return FileVisitResult.CONTINUE;
                }

                private FileVisitResult count(long size) {
                    entries[0]++;
                    bytes[0] += size;
                    return bytes[0] > maxBytes || entries[0] > maxEntries
                            ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // папку могли удалить после завершения программы
        }
        return new Usage(bytes[0], entries[0]);
    }

    record Usage(long bytes, int entries) {
    }

    /** Файлы после завершения программы: текстовые — с содержимым, остальные — только с размером. */
    public List<OutputFile> collect() {
        List<OutputFile> result = new ArrayList<>();
        try (Stream<Path> files = Files.walk(path)) {
            List<Path> regular = files
                    .filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
                    .sorted()
                    .limit(MAX_RETURNED_FILES)
                    .toList();
            for (Path file : regular) {
                String name = path.relativize(file).toString().replace('\\', '/');
                long size = Files.size(file);
                result.add(new OutputFile(name, size <= MAX_TEXT_BYTES ? text(file) : null, size));
            }
        } catch (IOException ignored) {
            // вернём то, что успели прочитать
        }
        return result;
    }

    private static String text(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return text.indexOf('\0') >= 0 ? null : text;
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    @Override
    public void close() {
        try (Stream<Path> files = Files.walk(path)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // уже удалена
        }
    }
}
