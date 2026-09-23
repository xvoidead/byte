package dev.byteide.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * Ограничение частоты запросов по IP (token bucket) и числа одновременных запусков с одного адреса.
 * Хранится в памяти: для одного сервера этого достаточно.
 */
@Component
public class RateLimiter {

    public enum Category { RUN, COMPILE, EVENTS }

    /** Результат попытки: разрешено или через сколько секунд повторить. */
    public record Decision(boolean allowed, long retryAfterSeconds) {
        static final Decision ALLOWED = new Decision(true, 0);
    }

    private static final int MAX_TRACKED = 50_000;
    private static final long IDLE_NANOS = 15L * 60 * 1_000_000_000;

    private final LimitsProperties limits;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> running = new ConcurrentHashMap<>();

    public RateLimiter(LimitsProperties limits) {
        this.limits = limits;
    }

    public Decision tryAcquire(String ip, Category category) {
        if (buckets.size() > MAX_TRACKED) {
            evictIdle();
        }
        Bucket bucket = buckets.computeIfAbsent(category + "|" + ip, key -> newBucket(category));
        return bucket.tryTake();
    }

    /** Занимает слот одновременного запуска для адреса; false — адрес уже запускает максимум программ. */
    public boolean acquireRun(String ip) {
        AtomicInteger count = running.computeIfAbsent(ip, key -> new AtomicInteger());
        if (count.incrementAndGet() > limits.concurrentRunsPerIp()) {
            count.decrementAndGet();
            return false;
        }
        return true;
    }

    public void releaseRun(String ip) {
        running.computeIfPresent(ip, (key, count) -> count.decrementAndGet() <= 0 ? null : count);
    }

    private Bucket newBucket(Category category) {
        return switch (category) {
            case RUN -> new Bucket(limits.runBurst(), limits.runsPerMinute());
            case COMPILE -> new Bucket(Math.max(20, limits.compilesPerMinute() / 3), limits.compilesPerMinute());
            case EVENTS -> new Bucket(Math.max(10, limits.eventsPerMinute() / 3), limits.eventsPerMinute());
        };
    }

    private void evictIdle() {
        long now = System.nanoTime();
        buckets.entrySet().removeIf(e -> now - e.getValue().lastRefill > IDLE_NANOS);
    }

    static final class Bucket {
        private final double capacity;
        private final double perNano;
        private double tokens;
        private long lastRefill;

        Bucket(int capacity, int perMinute) {
            this.capacity = capacity;
            this.perNano = perMinute / 60e9;
            this.tokens = capacity;
            this.lastRefill = System.nanoTime();
        }

        synchronized Decision tryTake() {
            long now = System.nanoTime();
            tokens = Math.min(capacity, tokens + (now - lastRefill) * perNano);
            lastRefill = now;
            if (tokens >= 1) {
                tokens -= 1;
                return Decision.ALLOWED;
            }
            long waitSeconds = (long) Math.ceil((1 - tokens) / perNano / 1e9);
            return new Decision(false, Math.max(1, waitSeconds));
        }
    }
}
