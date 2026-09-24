package dev.byteide.lessons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import java.util.random.RandomGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import dev.byteide.runner.CompilationResult;
import dev.byteide.runner.CompiledProgram;
import dev.byteide.runner.ExecutionResult;
import dev.byteide.runner.JavaCompilationService;
import dev.byteide.runner.ProcessExecutionService;
import dev.byteide.runner.RunStatus;
import dev.byteide.runner.SandboxHealth;

/**
 * Случайные скрытые тесты. Для каждого урока заранее генерируется набор входов по шаблону,
 * а ожидаемый вывод берётся из эталонного решения. К каждой проверке добавляется несколько
 * случайно выбранных тестов из этого набора — подогнать ответ под них нельзя.
 *
 * <p>Набор строится в фоне после старта сервера; пока он не готов, проверка идёт только по обычным тестам.
 */
@Service
public class RandomTestPool {

    private static final Logger log = LoggerFactory.getLogger(RandomTestPool.class);

    static final int POOL_SIZE = 12;
    static final String TEST_NAME = "Случайный тест";

    private final LessonRepository lessons;
    private final JavaCompilationService compiler;
    private final ProcessExecutionService executor;
    private final SandboxHealth sandbox;
    private final Map<String, List<LessonTest>> pools = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public RandomTestPool(LessonRepository lessons, JavaCompilationService compiler, ProcessExecutionService executor,
                          SandboxHealth sandbox) {
        this.lessons = lessons;
        this.compiler = compiler;
        this.executor = executor;
        this.sandbox = sandbox;
    }

    @EventListener(ApplicationReadyEvent.class)
    void warmUp() {
        // Долгая фоновая работа — на обычном потоке с низким приоритетом, чтобы не занимать носители виртуальных потоков.
        Thread.ofPlatform().daemon().name("random-tests-warmup").priority(Thread.MIN_PRIORITY).start(() -> {
            long start = System.nanoTime();
            int built = 0;
            for (Lesson lesson : lessons.findAll()) {
                if (lesson.random() == null) {
                    continue;
                }
                try {
                    pool(lesson);
                    built++;
                } catch (RuntimeException e) {
                    log.error("Не удалось построить случайные тесты для урока {}: {}", lesson.slug(), e.getMessage());
                }
            }
            log.info("Случайные тесты готовы для {} уроков за {} мс", built, (System.nanoTime() - start) / 1_000_000);
        });
    }

    /** Несколько случайных тестов для проверки; пустой список, если набор ещё не готов. */
    public List<LessonTest> sample(Lesson lesson) {
        if (lesson.random() == null) {
            return List.of();
        }
        List<LessonTest> pool = pools.get(lesson.slug());
        if (pool == null || pool.isEmpty()) {
            return List.of();
        }
        List<LessonTest> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        return List.copyOf(shuffled.subList(0, Math.min(lesson.random().count(), shuffled.size())));
    }

    /** Строит набор (или возвращает готовый). Бросает исключение, если эталонное решение падает на входе. */
    public List<LessonTest> pool(Lesson lesson) {
        if (lesson.random() == null) {
            return List.of();
        }
        List<LessonTest> ready = pools.get(lesson.slug());
        if (ready != null) {
            return ready;
        }
        // ReentrantLock, а не synchronized: внутри ждём процессы, а блокировка в synchronized
        // закрепляет виртуальный поток за потоком-носителем и может остановить весь сервер.
        lock.lock();
        try {
            ready = pools.get(lesson.slug());
            if (ready != null) {
                return ready;
            }
            List<LessonTest> built = build(lesson);
            pools.put(lesson.slug(), built);
            return built;
        } finally {
            lock.unlock();
        }
    }

    private List<LessonTest> build(Lesson lesson) {
        sandbox.ensureVerified();
        RandomInput generator = new RandomInput(lesson.random().input());
        RandomGenerator random = RandomGenerator.of("L64X128MixRandom");
        CompilationResult compilation = compiler.compile(lesson.solution());
        if (!compilation.success()) {
            throw new IllegalStateException("Эталонное решение не компилируется: " + compilation.diagnostics());
        }
        List<LessonTest> tests = new ArrayList<>();
        try (CompiledProgram program = compilation.program()) {
            for (int i = 0; i < POOL_SIZE; i++) {
                String input = generator.render(random);
                ExecutionResult result = executor.execute(program, input);
                if (result.status() != RunStatus.SUCCESS) {
                    throw new IllegalStateException("Эталонное решение завершилось со статусом " + result.status()
                            + " на входе:\n" + input + "\n" + result.stderr());
                }
                tests.add(new LessonTest(TEST_NAME, input, result.stdout(), true));
            }
        }
        return List.copyOf(tests);
    }
}
