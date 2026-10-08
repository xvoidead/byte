package dev.byteide.web;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.byteide.lessons.CheckResult;
import dev.byteide.lessons.Lesson;
import dev.byteide.lessons.LessonRepository;
import dev.byteide.lessons.Requirement;
import dev.byteide.lessons.SolutionChecker;
import dev.byteide.lessons.Step;
import dev.byteide.runner.Project;
import dev.byteide.runner.ProjectFile;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private static final Logger log = LoggerFactory.getLogger(LessonController.class);

    private final LessonRepository lessons;
    private final SolutionChecker checker;
    private final RequestLimits limits;

    public LessonController(LessonRepository lessons, SolutionChecker checker, RequestLimits limits) {
        this.lessons = lessons;
        this.checker = checker;
        this.limits = limits;
    }

    @GetMapping
    public List<LessonSummary> list() {
        return lessons.findAll().stream()
                .map(l -> new LessonSummary(l.slug(), l.order(), l.module(), l.title(), l.summary()))
                .toList();
    }

    @GetMapping("/{slug}")
    public LessonDetails get(@PathVariable String slug) {
        Lesson lesson = find(slug);
        List<Lesson> all = lessons.findAll();
        int index = all.indexOf(lesson);
        String prev = index > 0 ? all.get(index - 1).slug() : null;
        String next = index < all.size() - 1 ? all.get(index + 1).slug() : null;
        List<Example> examples = lesson.tests().stream()
                .filter(t -> !t.hidden())
                .map(t -> new Example(t.name(), t.stdin(), t.expectedOutput(), t.files(), t.expectedFiles()))
                .toList();
        int randomCount = lesson.random() == null ? 0 : lesson.random().count();
        return new LessonDetails(lesson.slug(), lesson.order(), lesson.module(), lesson.title(), lesson.summary(),
                lesson.steps(), lesson.task(), lesson.starterCode(), lesson.starter().files(), lesson.activeFile(), examples,
                lesson.tests().size() + randomCount,
                lesson.hints(), lesson.requirements().stream().map(Requirement::message).toList(), prev, next);
    }

    /**
     * Эталонное решение. Интерфейс показывает его после решения задачи или когда ученик открыл все подсказки
     * и несколько раз не прошёл проверку.
     */
    @GetMapping("/{slug}/solution")
    public Solution solution(@PathVariable String slug) {
        Lesson lesson = find(slug);
        String main = lesson.solution().sources().stream().filter(f -> f.name().equals(Project.MAIN_FILE))
                .findFirst().or(() -> lesson.solution().sources().stream().findFirst())
                .map(ProjectFile::content).orElse("");
        return new Solution(main, lesson.solution().files());
    }

    @PostMapping("/{slug}/check")
    public CheckResult check(@PathVariable String slug, @RequestBody CheckRequest request, HttpServletRequest http) {
        Project project = limits.project(request.code(), request.files());
        CheckResult result = checker.check(find(slug), project);
        log.info("check lesson={} passed={} compiled={} ip={}", slug, result.passed(), result.compiled(),
                ClientIp.masked(ClientIp.of(http)));
        return result;
    }

    private Lesson find(String slug) {
        return lessons.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Урок не найден"));
    }

    public record LessonSummary(String slug, int order, String module, String title, String summary) {
    }

    public record LessonDetails(
            String slug,
            int order,
            String module,
            String title,
            String summary,
            List<Step> steps,
            String task,
            String starterCode,
            List<ProjectFile> starterFiles,
            String activeFile,
            List<Example> examples,
            int testCount,
            List<String> hints,
            List<String> requirements,
            String prev,
            String next) {
    }

    /** {@code code} — главный файл решения (для старых клиентов), {@code files} — весь проект. */
    public record Solution(String code, List<ProjectFile> files) {
    }

    /** Открытый тест; {@code files}/{@code expectedFiles} — файлы до и после запуска, если тест их задаёт. */
    public record Example(String name, String stdin, String expectedOutput, Map<String, String> files,
                          Map<String, String> expectedFiles) {
    }

    public record CheckRequest(String code, List<ProjectFile> files) {
    }
}
