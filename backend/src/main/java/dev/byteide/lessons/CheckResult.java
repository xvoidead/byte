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

    public record TestOutcome(
            String name,
            boolean passed,
            boolean hidden,
            RunStatus status,
            String stdin,
            String expectedOutput,
            String actualOutput,
            String stderr) {
    }
}
