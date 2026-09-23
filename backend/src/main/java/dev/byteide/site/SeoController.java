package dev.byteide.site;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.byteide.lessons.Lesson;
import dev.byteide.lessons.LessonRepository;

@RestController
public class SeoController {

    private final SpaPage page;
    private final LessonRepository lessons;

    public SeoController(SpaPage page, LessonRepository lessons) {
        this.page = page;
        this.lessons = lessons;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String body = """
                User-agent: *
                Allow: /
                Disallow: /api/
                Disallow: /stats

                Sitemap: %s/sitemap.xml
                """.formatted(page.baseUrl());
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(body);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        String base = page.baseUrl();
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);
        url(xml, base + "/", "1.0");
        for (Lesson lesson : lessons.findAll()) {
            url(xml, base + "/lessons/" + lesson.slug(), "0.8");
        }
        url(xml, base + "/playground", "0.6");
        url(xml, base + "/privacy", "0.2");
        url(xml, base + "/terms", "0.2");
        xml.append("</urlset>\n");
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(xml.toString());
    }

    private static void url(StringBuilder xml, String loc, String priority) {
        xml.append("  <url><loc>").append(SpaPage.escape(loc)).append("</loc><priority>")
                .append(priority).append("</priority></url>\n");
    }
}
