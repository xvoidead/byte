package dev.byteide.lessons;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import dev.byteide.runner.CodeRunner;
import dev.byteide.runner.RunResult;
import dev.byteide.runner.RunStatus;

/**
 * Проверяет содержимое каждого урока: эталонное решение проходит все тесты и требования,
 * случайные тесты строятся, стартовый код не проходит, а ответы на вопросы «что выведет программа»
 * подтверждаются настоящим запуском.
 */
@SpringBootTest
class LessonsContentTest {

    private static final LessonRepository LESSONS = LessonRepositoryHolder.get();

    /**
     * Только часть уроков — чтобы быстро проверить те, над которыми идёт работа:
     * {@code mvn test -Dtest=LessonsContentTest -Dlessons=sqlite-jdbc,prepared-statements}.
     */
    private static final List<String> ONLY = System.getProperty("lessons", "").isBlank() ? List.of()
            : List.of(System.getProperty("lessons").split(","));

    private static Stream<Lesson> selected() {
        return LESSONS.findAll().stream().filter(lesson -> ONLY.isEmpty() || ONLY.contains(lesson.slug()));
    }

    @Autowired
    LessonRepository repository;

    @Autowired
    SolutionChecker checker;

    @Autowired
    RandomTestPool randomTests;

    @Autowired
    CodeRunner runner;

    static Stream<String> slugs() {
        return selected().map(Lesson::slug);
    }

    static Stream<Arguments> quizzes() {
        return selected().flatMap(lesson -> lesson.quizzes().stream()
                .filter(Quiz::asksForOutput)
                .map(quiz -> Arguments.of(lesson.slug(), quiz.question() + " / " + quiz.options().get(quiz.answer()), quiz)));
    }

    @Test
    void lessonsAreNumberedWithoutGapsAndComplete() {
        List<Lesson> all = repository.findAll();
        assertThat(all).extracting(Lesson::order).containsExactlyElementsOf(
                java.util.stream.IntStream.rangeClosed(1, all.size()).boxed().toList());
        assertThat(all).allSatisfy(lesson -> {
            assertThat(lesson.title()).isNotBlank();
            assertThat(lesson.module()).isNotBlank();
            assertThat(lesson.steps()).as(lesson.slug()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(lesson.task()).isNotBlank();
            assertThat(lesson.starterCode()).contains("public static void main");
            assertThat(lesson.solution().allSources()).contains("public static void main");
            assertThat(lesson.tests()).anyMatch(t -> !t.hidden());
            assertThat(lesson.hints()).as(lesson.slug() + ": подсказки").isNotEmpty();
            assertThat(lesson.quizzes()).as(lesson.slug() + ": вопросы").isNotEmpty();
        });
    }

    @ParameterizedTest
    @MethodSource("slugs")
    void referenceSolutionPassesTestsRequirementsAndRandomTests(String slug) {
        Lesson lesson = repository.findBySlug(slug).orElseThrow();

        List<LessonTest> pool = randomTests.pool(lesson);
        CheckResult result = checker.check(lesson, lesson.solution());

        if (lesson.random() != null) {
            assertThat(pool).hasSize(RandomTestPool.POOL_SIZE);
            assertThat(result.tests()).anyMatch(t -> t.name().equals(RandomTestPool.TEST_NAME));
        }
        assertThat(result.tests()).allSatisfy(t -> assertThat(t.passed()).as(t.name()).isTrue());
        assertThat(result.requirements()).allSatisfy(r -> assertThat(r.passed()).as(r.message()).isTrue());
        assertThat(result.passed()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("slugs")
    void starterCodeCompilesButDoesNotPass(String slug) {
        Lesson lesson = repository.findBySlug(slug).orElseThrow();

        CheckResult result = checker.check(lesson, lesson.starter());

        assertThat(result.compiled()).as("стартовый код должен компилироваться").isTrue();
        assertThat(result.passed()).isFalse();
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("quizzes")
    void quizAnswerMatchesRealOutput(String slug, String title, Quiz quiz) {
        String code = quiz.code().contains("class ") ? quiz.code() : """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) throws Exception {
                %s
                    }
                }
                """.formatted(quiz.code());

        RunResult result = runner.run(code, "");

        assertThat(result.status()).as(result.stderr() + result.diagnostics()).isEqualTo(RunStatus.SUCCESS);
        assertThat(SolutionChecker.normalize(result.stdout()))
                .isEqualTo(SolutionChecker.normalize(quiz.options().get(quiz.answer())));
    }

    /** Уроки нужны и в статических фабриках параметров, где Spring ещё не поднят. */
    static final class LessonRepositoryHolder {
        static LessonRepository get() {
            return new LessonRepository(tools.jackson.databind.json.JsonMapper.builder().build());
        }
    }
}
