package dev.byteide.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import dev.byteide.analytics.AnalyticsService.Event;

@SpringBootTest(properties = "byte.analytics.admin-token=test-secret")
@AutoConfigureMockMvc
class AnalyticsTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AnalyticsService analytics;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM events");
    }

    @Test
    void acceptsBeaconBatch() throws Exception {
        String visitor = UUID.randomUUID().toString();
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("""
                        {"visitor": "%s", "events": [
                          {"type": "lesson_open", "lesson": "input"},
                          {"type": "quiz_answer", "lesson": "input", "item": "2-1", "value": 1}
                        ]}
                        """.formatted(visitor)))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM events WHERE visitor = ?", Long.class, visitor)).isEqualTo(2);
    }

    @Test
    void dropsInvalidEvents() {
        String visitor = UUID.randomUUID().toString();
        int saved = analytics.record(visitor, List.of(
                new Event("lesson_open", "input", null, null),
                new Event("drop_table", "input", null, null),
                new Event("lesson_open", "no-such-lesson", null, null),
                new Event("test_failed", "input", "x".repeat(1000), null),
                new Event("quiz_answer", "input", "2-1", 1),
                new Event("quiz_answer", "input", "0-0", 1),
                new Event("quiz_answer", "input", "99-1", 1)));
        assertThat(saved).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT MAX(LENGTH(item)) FROM events", Integer.class)).isEqualTo(200);

        assertThat(analytics.record("not-a-uuid", List.of(new Event("lesson_open", "input", null, null)))).isZero();
        assertThat(analytics.record(visitor.toUpperCase(), List.of(new Event("lesson_open", "input", null, null)))).isZero();
    }

    @Test
    void rejectsOversizedBatch() throws Exception {
        String events = "{\"type\": \"page_view\"},".repeat(51);
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"visitor\": \"%s\", \"events\": [%s]}".formatted(UUID.randomUUID(), events.substring(0, events.length() - 1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statsRequireToken() throws Exception {
        mvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/stats").header("Authorization", "Bearer wrong")).andExpect(status().isUnauthorized());
    }

    @Test
    void buildsFunnelAndQuizStats() throws Exception {
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();
        analytics.record(first, List.of(
                new Event("lesson_open", "input", null, null),
                new Event("run", "input", "SUCCESS", null),
                new Event("check", "input", null, 0),
                new Event("test_failed", "input", "Скрытый тест 3", null),
                new Event("check", "input", null, 1),
                new Event("lesson_complete", "input", null, null),
                new Event("quiz_answer", "input", "2-1", 0),
                new Event("quiz_answer", "input", "2-1", 1),
                new Event("compile_error", "input", "compiler.err.cant.resolve", null)));
        analytics.record(second, List.of(
                new Event("lesson_open", "input", null, null),
                new Event("quiz_answer", "input", "2-1", 1),
                new Event("hint_open", "input", null, 1)));

        mvc.perform(get("/api/admin/stats").header("Authorization", "Bearer test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.visitors30d").value(2))
                .andExpect(jsonPath("$.totals.completions").value(1))
                .andExpect(jsonPath("$.daily.length()").value(30))
                .andExpect(jsonPath("$.lessons.length()").value(37))
                .andExpect(jsonPath("$.lessons[?(@.slug == 'input')].opened").value(2))
                .andExpect(jsonPath("$.lessons[?(@.slug == 'input')].started").value(1))
                .andExpect(jsonPath("$.lessons[?(@.slug == 'input')].completed").value(1))
                .andExpect(jsonPath("$.lessons[?(@.slug == 'input')].checksPerCompletion").value(2.0))
                .andExpect(jsonPath("$.lessons[?(@.slug == 'input')].hints").value(1))
                .andExpect(jsonPath("$.failingTests[0].item").value("Скрытый тест 3"))
                .andExpect(jsonPath("$.compileErrors[0].item").value("compiler.err.cant.resolve"))
                // Первый ответ первого посетителя неверный, второго — верный: 50 %.
                .andExpect(jsonPath("$.quizzes[0].item").value("2-1"))
                .andExpect(jsonPath("$.quizzes[0].firstTryCorrect").value(50))
                .andExpect(jsonPath("$.quizzes[0].question").isNotEmpty())
                .andExpect(jsonPath("$.quizzes[0].step").isNotEmpty());
    }

    @Test
    void deletesOldEvents() {
        String visitor = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO events (ts, event_day, visitor, type) VALUES (CURRENT_TIMESTAMP, DATEADD('DAY', -200, CURRENT_DATE), ?, 'page_view')",
                visitor);
        analytics.record(visitor, List.of(new Event("page_view", null, null, null)));
        analytics.cleanUp();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM events", Long.class)).isEqualTo(1);
    }
}
