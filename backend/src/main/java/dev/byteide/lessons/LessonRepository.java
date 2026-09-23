package dev.byteide.lessons;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Загружает уроки из classpath: {@code lessons/NN-slug/} с файлами
 * {@code lesson.json}, {@code theory.md}, {@code task.md} и {@code Main.java}.
 */
@Repository
public class LessonRepository {

    private static final Pattern DIR_NAME = Pattern.compile("(\\d+)-([a-z0-9-]+)");

    private final Map<String, Lesson> lessons;

    public LessonRepository(JsonMapper jsonMapper) {
        this.lessons = load(jsonMapper);
    }

    public List<Lesson> findAll() {
        return List.copyOf(lessons.values());
    }

    public Optional<Lesson> findBySlug(String slug) {
        return Optional.ofNullable(lessons.get(slug));
    }

    private static Map<String, Lesson> load(JsonMapper jsonMapper) {
        try {
            Resource[] metas = new PathMatchingResourcePatternResolver().getResources("classpath*:lessons/*/lesson.json");
            Map<String, Lesson> result = new LinkedHashMap<>();
            Arrays.stream(metas)
                    .map(meta -> read(jsonMapper, meta))
                    .sorted(Comparator.comparingInt(Lesson::order))
                    .forEach(lesson -> {
                        if (result.putIfAbsent(lesson.slug(), lesson) != null) {
                            throw new IllegalStateException("Повторяющийся урок: " + lesson.slug());
                        }
                    });
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Lesson read(JsonMapper jsonMapper, Resource meta) {
        try {
            String url = meta.getURL().toString();
            String parent = url.substring(0, url.lastIndexOf('/'));
            String dirName = parent.substring(parent.lastIndexOf('/') + 1);
            Matcher matcher = DIR_NAME.matcher(dirName);
            if (!matcher.matches()) {
                throw new IllegalStateException("Каталог урока должен называться NN-slug: " + dirName);
            }
            LessonMeta lessonMeta = jsonMapper.readValue(meta.getInputStream(), LessonMeta.class);
            return new Lesson(
                    matcher.group(2),
                    Integer.parseInt(matcher.group(1)),
                    lessonMeta.module(),
                    lessonMeta.title(),
                    lessonMeta.summary(),
                    text(meta.createRelative("theory.md")),
                    text(meta.createRelative("task.md")),
                    text(meta.createRelative("Main.java")),
                    lessonMeta.tests() == null ? List.of() : List.copyOf(lessonMeta.tests()));
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать урок " + meta, e);
        }
    }

    private static String text(Resource resource) throws IOException {
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private record LessonMeta(String module, String title, String summary, List<LessonTest> tests) {
    }
}
