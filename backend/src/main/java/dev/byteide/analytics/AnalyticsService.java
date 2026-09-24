package dev.byteide.analytics;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dev.byteide.lessons.Lesson;
import dev.byteide.lessons.LessonRepository;
import dev.byteide.lessons.Step;

/**
 * Анонимная аналитика: какие уроки открывают и заканчивают, где застревают, какие ошибки делают.
 * Посетитель — случайный идентификатор из браузера; IP, cookies и код учеников не сохраняются.
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    public static final Set<String> TYPES = Set.of(
            "page_view", "lesson_open", "step_view", "quiz_answer", "run", "compile_error", "check",
            "test_failed", "requirement_failed", "hint_open", "solution_open", "lesson_complete");

    static final int MAX_BATCH = 50;
    private static final int MAX_ITEM = 200;
    private static final Pattern VISITOR = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final JdbcTemplate jdbc;
    private final LessonRepository lessons;
    private final AnalyticsProperties properties;

    public AnalyticsService(JdbcTemplate jdbc, LessonRepository lessons, AnalyticsProperties properties) {
        this.jdbc = jdbc;
        this.lessons = lessons;
        this.properties = properties;
    }

    public record Event(String type, String lesson, String item, Integer value) {
    }

    /** Сохраняет пакет событий; некорректные события молча отбрасываются. Возвращает число сохранённых. */
    public int record(String visitor, List<Event> events) {
        if (visitor == null || !VISITOR.matcher(visitor).matches() || events == null) {
            return 0;
        }
        Instant now = Instant.now();
        Timestamp ts = Timestamp.from(now);
        Date day = Date.valueOf(LocalDate.ofInstant(now, ZoneOffset.UTC));
        List<Object[]> rows = new ArrayList<>();
        for (Event event : events.subList(0, Math.min(events.size(), MAX_BATCH))) {
            if (event == null || !TYPES.contains(event.type())) {
                continue;
            }
            if (event.lesson() != null && lessons.findBySlug(event.lesson()).isEmpty()) {
                continue;
            }
            if (event.type().equals("quiz_answer") && question(event.lesson(), event.item()) == null) {
                continue;
            }
            String item = event.item() == null ? null
                    : event.item().length() > MAX_ITEM ? event.item().substring(0, MAX_ITEM) : event.item();
            rows.add(new Object[]{ts, day, visitor, event.type(), event.lesson(), item, event.value()});
        }
        if (!rows.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO events (ts, event_day, visitor, type, lesson, item, val) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    rows);
        }
        return rows.size();
    }

    /** Ночная очистка: события старше срока хранения удаляются. */
    @Scheduled(cron = "0 17 3 * * *")
    public void cleanUp() {
        LocalDate border = LocalDate.now(ZoneOffset.UTC).minusDays(properties.retentionDays());
        int deleted = jdbc.update("DELETE FROM events WHERE event_day < ?", Date.valueOf(border));
        if (deleted > 0) {
            log.info("Удалено старых событий аналитики: {}", deleted);
        }
    }

    /** Сводка для страницы /stats. */
    public Map<String, Object> stats() {
        LocalDate since = LocalDate.now(ZoneOffset.UTC).minusDays(29);
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("totals", Map.of(
                "visitors30d", count("SELECT COUNT(DISTINCT visitor) FROM events WHERE event_day >= ?", Date.valueOf(since)),
                "visitorsToday", count("SELECT COUNT(DISTINCT visitor) FROM events WHERE event_day = ?",
                        Date.valueOf(LocalDate.now(ZoneOffset.UTC))),
                "completions", count("SELECT COUNT(*) FROM (SELECT DISTINCT visitor, lesson FROM events "
                        + "WHERE type = 'lesson_complete')"),
                "runs", count("SELECT COUNT(*) FROM events WHERE type = 'run'"),
                "checks", count("SELECT COUNT(*) FROM events WHERE type = 'check'")));

        Map<LocalDate, Long> daily = new HashMap<>();
        jdbc.query("SELECT event_day, COUNT(DISTINCT visitor) FROM events WHERE event_day >= ? GROUP BY event_day",
                rs -> {
                    daily.put(rs.getDate(1).toLocalDate(), rs.getLong(2));
                }, Date.valueOf(since));
        List<Map<String, Object>> days = new ArrayList<>();
        for (LocalDate d = since; !d.isAfter(LocalDate.now(ZoneOffset.UTC)); d = d.plusDays(1)) {
            days.add(Map.of("day", d.toString(), "visitors", daily.getOrDefault(d, 0L)));
        }
        result.put("daily", days);

        Map<String, Map<String, Long>> perLesson = new HashMap<>();
        jdbc.query("SELECT lesson, type, COUNT(DISTINCT visitor), COUNT(*) FROM events WHERE lesson IS NOT NULL "
                + "GROUP BY lesson, type", rs -> {
            Map<String, Long> counts = perLesson.computeIfAbsent(rs.getString(1), k -> new HashMap<>());
            counts.put(rs.getString(2), rs.getLong(3));
            counts.put(rs.getString(2) + "#all", rs.getLong(4));
        });
        Map<String, Long> started = new HashMap<>();
        jdbc.query("SELECT lesson, COUNT(DISTINCT visitor) FROM events WHERE type IN ('run', 'check') "
                + "AND lesson IS NOT NULL GROUP BY lesson", rs -> {
            started.put(rs.getString(1), rs.getLong(2));
        });
        List<Map<String, Object>> lessonRows = new ArrayList<>();
        for (Lesson lesson : lessons.findAll()) {
            Map<String, Long> counts = perLesson.getOrDefault(lesson.slug(), Map.of());
            long completed = counts.getOrDefault("lesson_complete", 0L);
            long checks = counts.getOrDefault("check#all", 0L);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("slug", lesson.slug());
            row.put("order", lesson.order());
            row.put("title", lesson.title());
            row.put("opened", counts.getOrDefault("lesson_open", 0L));
            row.put("started", started.getOrDefault(lesson.slug(), 0L));
            row.put("completed", completed);
            row.put("checksPerCompletion", completed == 0 ? null : Math.round(checks * 10.0 / completed) / 10.0);
            row.put("hints", counts.getOrDefault("hint_open", 0L));
            row.put("solutions", counts.getOrDefault("solution_open", 0L));
            lessonRows.add(row);
        }
        result.put("lessons", lessonRows);

        result.put("failingTests", top("test_failed", true));
        result.put("failingRequirements", top("requirement_failed", true));
        result.put("compileErrors", top("compile_error", false));
        result.put("quizzes", quizzes());
        return result;
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    /** Самые частые события одного вида: по числу посетителей или по числу событий. */
    private List<Map<String, Object>> top(String type, boolean distinctVisitors) {
        String measure = distinctVisitors ? "COUNT(DISTINCT visitor)" : "COUNT(*)";
        return jdbc.query("SELECT lesson, item, " + measure + " AS c FROM events WHERE type = ? AND item IS NOT NULL "
                        + "GROUP BY lesson, item ORDER BY c DESC LIMIT 20",
                (rs, i) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("lesson", rs.getString(1));
                    row.put("item", rs.getString(2));
                    row.put("count", rs.getLong(3));
                    return row;
                }, type);
    }

    /** Доля правильных ответов с первой попытки по каждому вопросу — самые трудные сверху. */
    private List<Map<String, Object>> quizzes() {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT lesson, item, COUNT(*), SUM(val) FROM (
                    SELECT lesson, item, visitor, val,
                           ROW_NUMBER() OVER (PARTITION BY lesson, item, visitor ORDER BY id) AS rn
                    FROM events WHERE type = 'quiz_answer' AND lesson IS NOT NULL AND item IS NOT NULL
                ) first_answers WHERE rn = 1 GROUP BY lesson, item
                """, (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            long visitors = rs.getLong(3);
            row.put("lesson", rs.getString(1));
            row.put("item", rs.getString(2));
            Question question = question(rs.getString(1), rs.getString(2));
            row.put("question", question == null ? rs.getString(2) : question.text());
            row.put("step", question == null ? null : question.step());
            row.put("visitors", visitors);
            row.put("firstTryCorrect", visitors == 0 ? 0 : Math.round(rs.getLong(4) * 100.0 / visitors));
            return row;
        });
        rows.sort((a, b) -> Long.compare((Long) a.get("firstTryCorrect"), (Long) b.get("firstTryCorrect")));
        return rows.subList(0, Math.min(rows.size(), 15));
    }

    private record Question(String step, String text) {
    }

    /** item вопроса — «шаг-блок»; по нему находим вопрос. null, если такого вопроса в уроке нет. */
    private Question question(String slug, String item) {
        if (slug == null || item == null) {
            return null;
        }
        try {
            String[] parts = item.split("-", 2);
            Lesson lesson = lessons.findBySlug(slug).orElseThrow();
            Step step = lesson.steps().get(Integer.parseInt(parts[0]));
            return step.blocks().get(Integer.parseInt(parts[1])) instanceof Step.Question question
                    ? new Question(step.title(), question.quiz().question())
                    : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
