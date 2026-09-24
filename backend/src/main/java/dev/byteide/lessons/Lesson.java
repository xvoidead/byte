package dev.byteide.lessons;

import java.util.List;

import dev.byteide.runner.Project;
import dev.byteide.runner.ProjectFile;

/**
 * Урок: шаги теории с вопросами, задание, стартовый код, эталонное решение, тесты,
 * подсказки и требования к устройству решения.
 *
 * @param starter  стартовый проект: {@code Main.java} или содержимое каталога {@code starter/}
 * @param solution эталонное решение: {@code solution.java} или содержимое каталога {@code solution/}
 * @param random   случайные скрытые тесты или null
 */
public record Lesson(
        String slug,
        int order,
        String module,
        String title,
        String summary,
        List<Step> steps,
        String task,
        Project starter,
        Project solution,
        List<LessonTest> tests,
        List<String> hints,
        List<Requirement> requirements,
        RandomTests random) {

    /** Случайные тесты: сколько добавлять к каждой проверке и шаблон ввода. */
    public record RandomTests(int count, String input) {
    }

    /** Главный исходник стартового проекта — для клиентов, которые умеют только один файл. */
    public String starterCode() {
        return starter.files().stream().filter(f -> f.name().equals(Project.MAIN_FILE)).findFirst()
                .or(() -> starter.sources().stream().findFirst())
                .map(ProjectFile::content).orElse("");
    }

    public List<Quiz> quizzes() {
        return steps.stream()
                .flatMap(step -> step.blocks().stream())
                .filter(Step.Question.class::isInstance)
                .map(block -> ((Step.Question) block).quiz())
                .toList();
    }
}
