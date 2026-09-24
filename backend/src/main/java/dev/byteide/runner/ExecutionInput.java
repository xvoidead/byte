package dev.byteide.runner;

import java.util.List;

/** Что получает программа при запуске: ввод с клавиатуры и файлы в рабочей папке. */
public record ExecutionInput(String stdin, List<ProjectFile> workFiles) {

    public ExecutionInput {
        stdin = stdin == null ? "" : stdin;
        workFiles = workFiles == null ? List.of() : List.copyOf(workFiles);
    }

    public static ExecutionInput of(String stdin) {
        return new ExecutionInput(stdin, List.of());
    }
}
