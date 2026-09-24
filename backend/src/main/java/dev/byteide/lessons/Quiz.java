package dev.byteide.lessons;

import java.util.List;

/**
 * Мини-вопрос внутри теории.
 *
 * @param question    вопрос (Markdown)
 * @param code        фрагмент кода к вопросу или null
 * @param options     варианты ответа
 * @param answer      индекс правильного варианта
 * @param explanation объяснение, которое показывается после ответа
 */
public record Quiz(String question, String code, List<String> options, int answer, String explanation) {

    /** Вопрос вида «Что выведет программа?» — правильный вариант проверяется запуском кода в тестах. */
    public boolean asksForOutput() {
        return code != null && question.toLowerCase().contains("выведет");
    }
}
