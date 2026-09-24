package dev.byteide.lessons;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.byteide.runner.CodeRunner;
import dev.byteide.runner.Diagnostic;
import dev.byteide.runner.RunResult;
import dev.byteide.runner.RunStatus;

@Service
public class SolutionChecker {

    private final CodeRunner runner;
    private final CodeInspector inspector;
    private final RandomTestPool randomTests;

    public SolutionChecker(CodeRunner runner, CodeInspector inspector, RandomTestPool randomTests) {
        this.runner = runner;
        this.inspector = inspector;
        this.randomTests = randomTests;
    }

    public CheckResult check(Lesson lesson, String source) {
        List<LessonTest> tests = new ArrayList<>(lesson.tests());
        tests.addAll(randomTests.sample(lesson));
        return runner.runEach(source, tests.stream().map(LessonTest::stdin).toList(),
                compilation -> new CheckResult(false, false, compilation.diagnostics(), List.of(), List.of()),
                results -> {
                    List<CheckResult.TestOutcome> outcomes = new ArrayList<>();
                    for (int i = 0; i < tests.size(); i++) {
                        outcomes.add(outcome(tests.get(i), results.get(i)));
                    }
                    List<CodeInspector.Outcome> requirements = inspector.check(source, lesson.requirements());
                    boolean passed = outcomes.stream().allMatch(CheckResult.TestOutcome::passed)
                            && requirements.stream().allMatch(CodeInspector.Outcome::passed);
                    List<Diagnostic> diagnostics = results.isEmpty() ? List.of() : results.getFirst().diagnostics();
                    return new CheckResult(passed, true, diagnostics, outcomes, requirements);
                });
    }

    private static CheckResult.TestOutcome outcome(LessonTest test, RunResult result) {
        boolean passed = result.status() == RunStatus.SUCCESS
                && normalize(result.stdout()).equals(normalize(test.expectedOutput()));
        if (test.hidden()) {
            return new CheckResult.TestOutcome(test.name(), passed, true, result.status(), null, null, null, null);
        }
        return new CheckResult.TestOutcome(test.name(), passed, false, result.status(),
                test.stdin(), test.expectedOutput(), result.stdout(), result.stderr());
    }

    /** Сравниваем вывод без учёта концов строк, пробелов в конце строк и пустых строк в конце. */
    static String normalize(String output) {
        String[] lines = output.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line.stripTrailing()).append('\n');
        }
        return sb.toString().stripTrailing();
    }
}
