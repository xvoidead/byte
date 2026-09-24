package dev.byteide.lessons;

import java.util.List;

import dev.byteide.runner.Diagnostic;
import dev.byteide.runner.RunStatus;

/**
 * Итог проверки решения. Для скрытых тестов ввод и ожидаемый вывод не раскрываются.
 *
 * @param requirements выполнены ли требования к устройству решения (пусто, если код не скомпилировался)
 */
public record CheckResult(
        boolean passed,
        boolean compiled,
        List<Diagnostic> diagnostics,
        List<TestOutcome> tests,
        List<CodeInspector.Outcome> requirements) {

    /**
     * @param files проверка файлов после запуска; null для скрытых тестов и тестов без проверки файлов
     */
    public record TestOutcome(
            String name,
            boolean passed,
            boolean hidden,
            RunStatus status,
            String stdin,
            String expectedOutput,
            String actualOutput,
            String stderr,
            List<FileOutcome> files) {
    }

    /** Файл после запуска: каким должен быть и каким получился (null — файла нет или он не текстовый). */
    public record FileOutcome(String name, boolean passed, String expected, String actual) {
    }
}
