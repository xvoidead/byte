package dev.byteide.lessons;

import java.util.List;

/**
 * Урок: шаги теории с вопросами, задание, стартовый код, эталонное решение, тесты,
 * подсказки и требования к устройству решения.
 *
 * @param random случайные скрытые тесты или null
 */
public record Lesson(
        String slug,
        int order,
        String module,
        String title,
        String summary,
        List<Step> steps,
        String task,
        String starterCode,
        String solution,
        List<LessonTest> tests,
        List<String> hints,
        List<Requirement> requirements,
        RandomTests random) {

    /** Случайные тесты: сколько добавлять к каждой проверке и шаблон ввода. */
    public record RandomTests(int count, String input) {
    }

    public List<Quiz> quizzes() {
        return steps.stream()
                .flatMap(step -> step.blocks().stream())
                .filter(Step.Question.class::isInstance)
                .map(block -> ((Step.Question) block).quiz())
                .toList();
    }
}
