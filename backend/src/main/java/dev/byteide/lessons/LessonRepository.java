package dev.byteide.lessons;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

import dev.byteide.runner.Project;
import dev.byteide.runner.ProjectFile;
import tools.jackson.databind.json.JsonMapper;

/**
 * Загружает уроки из classpath: {@code lessons/NN-slug/} с файлами
 * {@code lesson.json}, {@code theory.md}, {@code task.md}, {@code Main.java} (стартовый код)
 * и {@code solution.java} (эталонное решение — для разбора и случайных тестов).
 *
 * <p>Урок-проект из нескольких файлов вместо {@code Main.java} и {@code solution.java} содержит каталоги
 * {@code starter/} и {@code solution/} с файлами проекта (например, {@code Main.java}, {@code Config.java},
 * {@code config.yml}, {@code resources/defaults.yml}).
 */
@Repository
public class LessonRepository {

    private static final Pattern DIR_NAME = Pattern.compile("(\\d+)-([a-z0-9-]+)");

    private final Map<String, Lesson> lessons;

    @org.springframework.beans.factory.annotation.Autowired
    public LessonRepository(JsonMapper jsonMapper) {
        this(jsonMapper, "lessons");
    }

    /** @param root каталог уроков в classpath (в тестах — отдельный набор) */
    LessonRepository(JsonMapper jsonMapper, String root) {
        this.lessons = load(jsonMapper, root);
    }

    public List<Lesson> findAll() {
        return List.copyOf(lessons.values());
    }

    public Optional<Lesson> findBySlug(String slug) {
        return Optional.ofNullable(lessons.get(slug));
    }

    private static Map<String, Lesson> load(JsonMapper jsonMapper, String root) {
        try {
            Resource[] metas = new PathMatchingResourcePatternResolver().getResources("classpath*:" + root + "/*/lesson.json");
            Map<String, Lesson> result = new LinkedHashMap<>();
            Arrays.stream(metas)
                    .map(meta -> read(jsonMapper, root, meta))
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

    private static Lesson read(JsonMapper jsonMapper, String root, Resource meta) {
        try {
            String url = meta.getURL().toString();
            String parent = url.substring(0, url.lastIndexOf('/'));
            String dirName = parent.substring(parent.lastIndexOf('/') + 1);
            Matcher matcher = DIR_NAME.matcher(dirName);
            if (!matcher.matches()) {
                throw new IllegalStateException("Каталог урока должен называться NN-slug: " + dirName);
            }
            LessonMeta lessonMeta = jsonMapper.readValue(meta.getInputStream(), LessonMeta.class);
            if (lessonMeta.random() != null) {
                new RandomInput(lessonMeta.random().input());
            }
            return new Lesson(
                    matcher.group(2),
                    Integer.parseInt(matcher.group(1)),
                    lessonMeta.module(),
                    lessonMeta.title(),
                    lessonMeta.summary(),
                    LessonMarkdown.parseSteps(text(meta.createRelative("theory.md"))),
                    text(meta.createRelative("task.md")),
                    project(root, dirName, "starter", "Main.java", meta),
                    project(root, dirName, "solution", "solution.java", meta),
                    listOf(lessonMeta.tests()),
                    listOf(lessonMeta.hints()),
                    listOf(lessonMeta.requirements()),
                    lessonMeta.random());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Ошибка в уроке " + meta + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать урок " + meta, e);
        }
    }

    /** Каталог {@code starter/} или {@code solution/}, а если его нет — один файл, который становится Main.java. */
    private static Project project(String root, String dirName, String dir, String singleFile, Resource meta)
            throws IOException {
        String prefix = root + "/" + dirName + "/" + dir + "/";
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:" + prefix + "**");
        List<ProjectFile> files = new ArrayList<>();
        for (Resource resource : resources) {
            String url = resource.getURL().toString();
            int at = url.lastIndexOf(prefix);
            String name = at < 0 ? "" : url.substring(at + prefix.length());
            if (name.isEmpty() || name.endsWith("/") || !resource.isReadable()) {
                continue;
            }
            files.add(new ProjectFile(name, text(resource)));
        }
        if (files.isEmpty()) {
            return Project.single(text(meta.createRelative(singleFile)));
        }
        files.sort(Comparator.comparing(ProjectFile::name));
        Project project = new Project(files);
        String problem = project.problem(Integer.MAX_VALUE);
        if (problem != null) {
            throw new IllegalArgumentException(dir + "/: " + problem);
        }
        return project;
    }

    private static String text(Resource resource) throws IOException {
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private static <T> List<T> listOf(List<T> list) {
        return list == null ? List.of() : List.copyOf(list);
    }

    private record LessonMeta(String module, String title, String summary, List<LessonTest> tests,
                              List<String> hints, List<Requirement> requirements, Lesson.RandomTests random) {
    }
}
