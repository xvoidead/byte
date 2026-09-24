package dev.byteide.lessons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class LessonMarkdownTest {

    @Test
    void splitsTheoryIntoStepsByH2AndParsesQuizzes() {
        List<Step> steps = LessonMarkdown.parseSteps("""
                Вступление.

                ## Первый шаг

                Текст.

                ```java
                // ## это не заголовок, а комментарий в коде
                int a = 1;
                ```

                ```quiz
                ? Что выведет программа?
                    System.out.println(1 + 1);
                - 11
                + 2
                > Числа складываются.
                ```

                ## Второй шаг

                Ещё текст.
                """);

        assertThat(steps).extracting(Step::title).containsExactly("Введение", "Первый шаг", "Второй шаг");
        assertThat(steps.get(1).blocks()).hasSize(2);
        Quiz quiz = ((Step.Question) steps.get(1).blocks().get(1)).quiz();
        assertThat(quiz.question()).isEqualTo("Что выведет программа?");
        assertThat(quiz.code()).isEqualTo("System.out.println(1 + 1);");
        assertThat(quiz.options()).containsExactly("11", "2");
        assertThat(quiz.answer()).isEqualTo(1);
        assertThat(quiz.explanation()).isEqualTo("Числа складываются.");
        assertThat(quiz.asksForOutput()).isTrue();
        assertThat(((Step.Text) steps.get(1).blocks().get(0)).markdown()).contains("// ## это не заголовок");
    }

    @Test
    void supportsMultilineOptions() {
        Quiz quiz = LessonMarkdown.parseQuiz(List.of("? Вывод?", "+ 1\\n2", "- 12"));

        assertThat(quiz.options().getFirst()).isEqualTo("1\n2");
    }

    @Test
    void rejectsQuizWithoutSingleCorrectAnswer() {
        assertThatThrownBy(() -> LessonMarkdown.parseQuiz(List.of("? Вопрос", "- a", "- b")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LessonMarkdown.parseQuiz(List.of("? Вопрос", "+ a", "+ b")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
