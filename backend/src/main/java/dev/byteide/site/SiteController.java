package dev.byteide.site;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.byteide.lessons.LessonRepository;
import dev.byteide.runner.SandboxHealth;

@RestController
@RequestMapping("/api")
public class SiteController {

    private final SiteProperties site;
    private final SandboxHealth sandbox;
    private final LessonRepository lessons;

    public SiteController(SiteProperties site, SandboxHealth sandbox, LessonRepository lessons) {
        this.site = site;
        this.sandbox = sandbox;
        this.lessons = lessons;
    }

    /** Публичные настройки для фронтенда. */
    @GetMapping("/site")
    public Map<String, Object> site() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contactEmail", blankToNull(site.contactEmail()));
        return result;
    }

    /** Для мониторинга и проверок хостинга: 503, если песочница не работает. */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Boolean verified = sandbox.status();
        String sandboxStatus = verified == null ? "checking" : verified ? "ok" : "failed";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", Boolean.FALSE.equals(verified) ? "degraded" : "ok");
        body.put("sandbox", sandboxStatus);
        body.put("lessons", lessons.findAll().size());
        HttpStatus status = Boolean.FALSE.equals(verified) ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.OK;
        return ResponseEntity.status(status).body(body);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
