package dev.byteide.analytics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AnalyticsService analytics;
    private final AnalyticsProperties properties;

    public AnalyticsController(AnalyticsService analytics, AnalyticsProperties properties) {
        this.analytics = analytics;
        this.properties = properties;
    }

    public record Batch(String visitor, List<AnalyticsService.Event> events) {
    }

    /** Пакет событий из браузера (в том числе через navigator.sendBeacon). */
    @PostMapping("/events")
    public ResponseEntity<Void> events(@RequestBody Batch batch) {
        if (batch.events() != null && batch.events().size() > AnalyticsService.MAX_BATCH) {
            return ResponseEntity.badRequest().build();
        }
        analytics.record(batch.visitor(), batch.events());
        return ResponseEntity.noContent().build();
    }

    /** Сводка для владельца сайта. Без настроенного токена страница не существует. */
    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> stats(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = properties.adminToken();
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Не найдено"));
        }
        String presented = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : "";
        if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Неверный токен"));
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(analytics.stats());
    }
}
