package dev.byteide.web;

import java.util.List;

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
import dev.byteide.lessons.SolutionChecker;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

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
                .map(t -> new Example(t.name(), t.stdin(), t.expectedOutput()))
                .toList();
        return new LessonDetails(lesson.slug(), lesson.order(), lesson.module(), lesson.title(), lesson.summary(),
                lesson.theory(), lesson.task(), lesson.starterCode(), examples, lesson.tests().size(), prev, next);
    }

    @PostMapping("/{slug}/check")
    public CheckResult check(@PathVariable String slug, @RequestBody CheckRequest request) {
        limits.checkSource(request.code());
        return checker.check(find(slug), request.code());
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
            String theory,
            String task,
            String starterCode,
            List<Example> examples,
            int testCount,
            String prev,
            String next) {
    }

    public record Example(String name, String stdin, String expectedOutput) {
    }

    public record CheckRequest(String code) {
    }
}
