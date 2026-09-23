package dev.byteide.site;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dev.byteide.lessons.Lesson;
import dev.byteide.lessons.LessonRepository;

/** Страницы фронтенда: отдаёт index.html с мета-тегами, чтобы работали прямые ссылки и превью. */
@Controller
public class SpaController {

    private final SpaPage page;
    private final LessonRepository lessons;

    public SpaController(SpaPage page, LessonRepository lessons) {
        this.page = page;
        this.lessons = lessons;
    }

    @GetMapping({"/", "/index.html"})
    public ResponseEntity<String> home() {
        return page.render(HttpStatus.OK, new SpaPage.Meta(SpaPage.DEFAULT_TITLE, SpaPage.DEFAULT_DESCRIPTION));
    }

    @GetMapping("/lessons/{slug}")
    public ResponseEntity<String> lesson(@PathVariable String slug) {
        return lessons.findBySlug(slug)
                .map(lesson -> page.render(HttpStatus.OK, meta(lesson)))
                .orElseGet(() -> notFound());
    }

    @GetMapping("/playground")
    public ResponseEntity<String> playground() {
        return page.render(HttpStatus.OK, new SpaPage.Meta("Песочница — byte",
                "Пишите и запускайте любой код на Java прямо в браузере, без установки."));
    }

    @GetMapping("/privacy")
    public ResponseEntity<String> privacy() {
        return page.render(HttpStatus.OK, new SpaPage.Meta("Конфиденциальность — byte",
                "Какие данные собирает byte и как их использует."));
    }

    @GetMapping("/terms")
    public ResponseEntity<String> terms() {
        return page.render(HttpStatus.OK, new SpaPage.Meta("Правила использования — byte",
                "Правила использования сайта byte."));
    }

    @GetMapping("/stats")
    public ResponseEntity<String> stats() {
        return page.render(HttpStatus.OK, new SpaPage.Meta("Статистика — byte", SpaPage.DEFAULT_DESCRIPTION));
    }

    ResponseEntity<String> notFound() {
        return page.render(HttpStatus.NOT_FOUND, new SpaPage.Meta("Страница не найдена — byte", SpaPage.DEFAULT_DESCRIPTION));
    }

    static SpaPage.Meta meta(Lesson lesson) {
        return new SpaPage.Meta(lesson.title() + " — урок " + lesson.order() + " · byte",
                lesson.summary() + " Урок курса Java с IDE в браузере.");
    }
}
