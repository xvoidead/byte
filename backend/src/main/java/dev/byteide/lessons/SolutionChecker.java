package dev.byteide.lessons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import dev.byteide.runner.CodeRunner;
import dev.byteide.runner.Diagnostic;
import dev.byteide.runner.ExecutionInput;
import dev.byteide.runner.OutputFile;
import dev.byteide.runner.Project;
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
        return check(lesson, Project.single(source));
    }

    public CheckResult check(Lesson lesson, Project project) {
        List<LessonTest> tests = new ArrayList<>(lesson.tests());
        tests.addAll(randomTests.sample(lesson));
        List<ExecutionInput> inputs = tests.stream()
                .map(t -> new ExecutionInput(t.stdin(), t.workFiles(project.workFiles())))
                .toList();
        return runner.runEach(project, inputs,
                compilation -> new CheckResult(false, false, compilation.diagnostics(), List.of(), List.of()),
                results -> {
                    List<CheckResult.TestOutcome> outcomes = new ArrayList<>();
                    for (int i = 0; i < tests.size(); i++) {
                        outcomes.add(outcome(tests.get(i), results.get(i)));
                    }
                    List<CodeInspector.Outcome> requirements = inspector.check(project, lesson.requirements());
                    boolean passed = outcomes.stream().allMatch(CheckResult.TestOutcome::passed)
                            && requirements.stream().allMatch(CodeInspector.Outcome::passed);
                    List<Diagnostic> diagnostics = results.isEmpty() ? List.of() : results.getFirst().diagnostics();
                    return new CheckResult(passed, true, diagnostics, outcomes, requirements);
                });
    }

    private static CheckResult.TestOutcome outcome(LessonTest test, RunResult result) {
        boolean outputOk = test.expectedOutput() == null
                || normalize(result.stdout()).equals(normalize(test.expectedOutput()));
        List<CheckResult.FileOutcome> files = files(test.expectedFiles(), result.files());
        boolean passed = result.status() == RunStatus.SUCCESS && outputOk
                && files.stream().allMatch(CheckResult.FileOutcome::passed);
        if (test.hidden()) {
            return new CheckResult.TestOutcome(test.name(), passed, true, result.status(), null, null, null, null, null);
        }
        return new CheckResult.TestOutcome(test.name(), passed, false, result.status(),
                test.stdin(), test.expectedOutput(), result.stdout(), result.stderr(), files.isEmpty() ? null : files);
    }

    private static List<CheckResult.FileOutcome> files(Map<String, String> expected, List<OutputFile> actual) {
        if (expected == null) {
            return List.of();
        }
        List<CheckResult.FileOutcome> outcomes = new ArrayList<>();
        expected.forEach((name, content) -> {
            String got = actual.stream().filter(f -> f.name().equals(name)).map(OutputFile::content)
                    .filter(Objects::nonNull).findFirst().orElse(null);
            boolean passed = got != null && normalize(got).equals(normalize(content));
            outcomes.add(new CheckResult.FileOutcome(name, passed, content, got));
        });
        return outcomes;
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
