package dev.byteide.runner;

import java.util.List;

/**
 * Итог выполнения программы.
 *
 * @param files файлы рабочей папки после завершения программы
 */
public record ExecutionResult(RunStatus status, String stdout, String stderr, Integer exitCode, long timeMs,
                              List<OutputFile> files) {

    public ExecutionResult {
        files = files == null ? List.of() : List.copyOf(files);
    }
}
