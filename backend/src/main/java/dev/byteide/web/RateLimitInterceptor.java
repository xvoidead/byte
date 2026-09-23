package dev.byteide.web;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Применяет ограничения частоты к API, которое запускает или компилирует код. */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String RUN_SLOT = RateLimitInterceptor.class.getName() + ".runSlot";

    private final RateLimiter limiter;

    public RateLimitInterceptor(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        RateLimiter.Category category = categoryOf(request);
        if (category == null) {
            return true;
        }
        String ip = ClientIp.of(request);
        RateLimiter.Decision decision = limiter.tryAcquire(ip, category);
        if (!decision.allowed()) {
            reject(response, decision.retryAfterSeconds(), switch (category) {
                case RUN -> "Слишком много запусков подряд. Подождите " + decision.retryAfterSeconds() + " с.";
                case COMPILE, EVENTS -> "Слишком много запросов. Подождите немного.";
            });
            return false;
        }
        if (category == RateLimiter.Category.RUN) {
            if (!limiter.acquireRun(ip)) {
                reject(response, 2, "Дождитесь завершения предыдущего запуска.");
                return false;
            }
            request.setAttribute(RUN_SLOT, ip);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        Object ip = request.getAttribute(RUN_SLOT);
        if (ip != null) {
            limiter.releaseRun((String) ip);
        }
    }

    private static RateLimiter.Category categoryOf(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        if (path.equals("/api/run") || (path.startsWith("/api/lessons/") && path.endsWith("/check"))) {
            return RateLimiter.Category.RUN;
        }
        if (path.equals("/api/compile")) {
            return RateLimiter.Category.COMPILE;
        }
        if (path.equals("/api/events")) {
            return RateLimiter.Category.EVENTS;
        }
        return null;
    }

    static void reject(HttpServletResponse response, long retryAfter, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + message.replace("\"", "'") + "\"}");
    }
}
