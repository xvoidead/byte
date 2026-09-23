package dev.byteide.lessons;

import java.util.List;

/**
 * Урок: теория, задание, стартовый код и тесты для проверки решения.
 */
public record Lesson(
        String slug,
        int order,
        String module,
        String title,
        String summary,
        String theory,
        String task,
        String starterCode,
        List<LessonTest> tests) {
}
