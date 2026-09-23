package dev.byteide.site;

import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/** Несуществующий адрес: для API — JSON, для страниц — фронтенд со статусом 404 и своей страницей «не найдено». */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NotFoundHandler {

    private final SpaController spa;

    public NotFoundHandler(SpaController spa) {
        this.spa = spa;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<?> notFound(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/") || !"GET".equals(request.getMethod())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Не найдено"));
        }
        return spa.notFound();
    }
}
