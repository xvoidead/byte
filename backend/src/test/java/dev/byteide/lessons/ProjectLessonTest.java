package dev.byteide.lessons;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import dev.byteide.runner.Project;
import dev.byteide.runner.ProjectFile;
import dev.byteide.runner.RunStatus;
import tools.jackson.databind.json.JsonMapper;

/** Урок-проект: стартовые файлы, ресурсы, файлы в тестах и проверка файлов после запуска. */
@SpringBootTest
class ProjectLessonTest {

    @Autowired
    JsonMapper json;

    @Autowired
    SolutionChecker checker;

    private Lesson lesson() {
        return new LessonRepository(json, "test-lessons").findBySlug("config-demo").orElseThrow();
    }

    @Test
    void loadsStarterAndSolutionDirectories() {
        Lesson lesson = lesson();

        assertThat(lesson.starter().files()).extracting(ProjectFile::name)
                .containsExactly("Config.java", "Main.java", "resources/config.yml");
        assertThat(lesson.solution().files()).extracting(ProjectFile::name)
                .containsExactly("Config.java", "Main.java", "resources/config.yml");
        assertThat(lesson.starterCode()).contains("Config.load()");
        assertThat(lesson.tests().get(1).files()).isEmpty();
    }

    @Test
    void referenceSolutionPassesFileTests() {
        CheckResult result = checker.check(lesson(), lesson().solution());

        assertThat(result.tests()).allSatisfy(t -> assertThat(t.passed()).as(t.name() + " " + t.stderr()).isTrue());
        assertThat(result.passed()).isTrue();
        CheckResult.TestOutcome created = result.tests().get(1);
        assertThat(created.files()).singleElement().satisfies(f -> {
            assertThat(f.name()).isEqualTo("config.yml");
            assertThat(f.actual()).isEqualTo("greeting: Привет\nmax-players: 20\n");
        });
        assertThat(result.tests().get(2).files()).as("данные скрытого теста не раскрываются").isNull();
    }

    @Test
    void starterFailsAndShowsFileDifference() {
        CheckResult result = checker.check(lesson(), lesson().starter());

        assertThat(result.passed()).isFalse();
        CheckResult.TestOutcome created = result.tests().get(1);
        assertThat(created.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(created.files()).singleElement().satisfies(f -> {
            assertThat(f.passed()).isFalse();
            assertThat(f.actual()).isNull();
        });
    }

    @Test
    void requirementsSeeAllFiles() {
        Project withoutConfigClass = new Project(List.of(new ProjectFile("Main.java", "public class Main { public static void main(String[] a) {} }")));

        CheckResult result = checker.check(lesson(), withoutConfigClass);

        assertThat(result.requirements()).singleElement().satisfies(r -> assertThat(r.passed()).isFalse());
        assertThat(checker.check(lesson(), lesson().starter()).requirements())
                .singleElement().satisfies(r -> assertThat(r.passed()).isTrue());
    }
}
