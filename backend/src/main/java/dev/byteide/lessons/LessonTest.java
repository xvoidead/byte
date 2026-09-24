package dev.byteide.lessons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.byteide.runner.ProjectFile;

/**
 * Тест задания: программа получает {@code stdin} (и файлы {@code files} в рабочей папке) и должна напечатать
 * {@code expectedOutput} и оставить после себя файлы {@code expectedFiles}.
 * Скрытые тесты не показываются ученику до проверки, а их данные не раскрываются и после.
 *
 * @param expectedOutput ожидаемый вывод; null — вывод не проверяется (только если заданы {@code expectedFiles})
 * @param files          файлы в рабочей папке перед запуском; null — те, что в проекте ученика
 * @param expectedFiles  какими должны быть файлы после запуска; null — файлы не проверяются
 */
public record LessonTest(String name, String stdin, String expectedOutput, boolean hidden,
                         Map<String, String> files, Map<String, String> expectedFiles) {

    public LessonTest {
        stdin = stdin == null ? "" : stdin;
        if (expectedOutput == null && expectedFiles == null) {
            expectedOutput = "";
        }
    }

    public LessonTest(String name, String stdin, String expectedOutput, boolean hidden) {
        this(name, stdin, expectedOutput, hidden, null, null);
    }

    /** Файлы рабочей папки для этого теста. */
    List<ProjectFile> workFiles(List<ProjectFile> fromProject) {
        if (files == null) {
            return fromProject;
        }
        List<ProjectFile> result = new ArrayList<>();
        files.forEach((name, content) -> result.add(new ProjectFile(name, content)));
        return result;
    }
}
