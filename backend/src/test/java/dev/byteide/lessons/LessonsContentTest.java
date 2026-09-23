package dev.byteide.lessons;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Проверяет, что эталонное решение каждого урока проходит тесты, а стартовый код — нет. */
@SpringBootTest
class LessonsContentTest {

    @Autowired
    LessonRepository repository;

    @Autowired
    SolutionChecker checker;

    static Stream<String> slugs() {
        return Stream.of("hello-world", "variables", "input", "conditions", "loops",
                "arrays", "strings", "methods", "classes", "collections");
    }

    @Test
    void loadsAllLessonsInOrder() {
        assertThat(repository.findAll()).extracting(Lesson::slug).containsExactlyElementsOf(slugs().toList());
        assertThat(repository.findAll()).allSatisfy(lesson -> {
            assertThat(lesson.title()).isNotBlank();
            assertThat(lesson.module()).isNotBlank();
            assertThat(lesson.theory()).isNotBlank();
            assertThat(lesson.task()).isNotBlank();
            assertThat(lesson.starterCode()).contains("public static void main");
            assertThat(lesson.tests()).isNotEmpty();
            assertThat(lesson.tests()).anyMatch(t -> !t.hidden());
        });
    }

    @ParameterizedTest
    @MethodSource("slugs")
    void referenceSolutionPasses(String slug) throws IOException {
        Lesson lesson = repository.findBySlug(slug).orElseThrow();

        CheckResult result = checker.check(lesson, solution(slug));

        assertThat(result.tests()).allSatisfy(t -> assertThat(t.passed()).as(t.name()).isTrue());
        assertThat(result.passed()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("slugs")
    void starterCodeCompilesButDoesNotPass(String slug) {
        Lesson lesson = repository.findBySlug(slug).orElseThrow();

        CheckResult result = checker.check(lesson, lesson.starterCode());

        assertThat(result.compiled()).as("стартовый код должен компилироваться").isTrue();
        assertThat(result.passed()).isFalse();
    }

    private static String solution(String slug) throws IOException {
        try (InputStream in = LessonsContentTest.class.getResourceAsStream("/solutions/" + slug + ".java")) {
            assertThat(in).as("эталонное решение для " + slug).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
