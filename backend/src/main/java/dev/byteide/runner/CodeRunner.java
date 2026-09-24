package dev.byteide.runner;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

/**
 * Точка входа для выполнения кода: компиляция + запуск с ограничением числа одновременных программ.
 */
@Service
public class CodeRunner {

    private final JavaCompilationService compiler;
    private final ProcessExecutionService executor;
    private final SandboxHealth sandboxHealth;
    private final RunnerProperties properties;
    private final Semaphore slots;
    private final Semaphore interactiveSlots;
    private final Semaphore compileSlots;

    public CodeRunner(JavaCompilationService compiler, ProcessExecutionService executor, SandboxHealth sandboxHealth,
                      RunnerProperties properties) {
        this.compiler = compiler;
        this.executor = executor;
        this.sandboxHealth = sandboxHealth;
        this.properties = properties;
        this.slots = new Semaphore(properties.maxConcurrentRuns(), true);
        this.interactiveSlots = new Semaphore(properties.maxInteractiveSessions());
        this.compileSlots = new Semaphore(properties.maxConcurrentCompiles());
    }

    /** Итог попытки интерактивного запуска: либо ошибка компиляции, либо запущенная программа. */
    public sealed interface Interactive {
        CompilationResult compilation();
    }

    public record CompilationFailed(CompilationResult compilation) implements Interactive {
    }

    public record Started(CompilationResult compilation, RunningProgram program) implements Interactive {
    }

    /**
     * Интерактивный запуск для консоли: программа получает ввод по ходу работы.
     * Занимает отдельный слот, который освобождается, когда программа завершится.
     */
    public Interactive startInteractive(String source, RunningProgram.Listener listener) {
        sandboxHealth.ensureVerified();
        if (!interactiveSlots.tryAcquire()) {
            throw new RunnerBusyException();
        }
        boolean started = false;
        try {
            CompilationResult compilation = compileWithSlot(source);
            if (!compilation.success()) {
                return new CompilationFailed(compilation);
            }
            RunningProgram.Listener releasing = new RunningProgram.Listener() {
                @Override
                public void onOutput(RunningProgram.Stream stream, String text) {
                    listener.onOutput(stream, text);
                }

                @Override
                public void onExit(ExecutionResult result) {
                    try {
                        listener.onExit(result);
                    } finally {
                        interactiveSlots.release();
                    }
                }
            };
            RunningProgram program = executor.startInteractive(compilation.program(), releasing);
            started = true;
            return new Started(compilation, program);
        } finally {
            if (!started) {
                interactiveSlots.release();
            }
        }
    }

    /** Проверка на ошибки для подсветки во время набора: без запуска и без записи на диск. */
    public JavaCompilationService.DiagnosticsResult diagnose(String source) {
        acquire(compileSlots, 2_000);
        try {
            return compiler.diagnose(source);
        } finally {
            compileSlots.release();
        }
    }

    private CompilationResult compileWithSlot(String source) {
        acquire(compileSlots, properties.queueTimeout().toMillis());
        try {
            return compiler.compile(source);
        } finally {
            compileSlots.release();
        }
    }

    private static void acquire(Semaphore semaphore, long waitMillis) {
        try {
            if (!semaphore.tryAcquire(waitMillis, TimeUnit.MILLISECONDS)) {
                throw new RunnerBusyException();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RunnerBusyException();
        }
    }

    public RunResult run(String source, String stdin) {
        return withSlot(() -> {
            CompilationResult compilation = compiler.compile(source);
            if (!compilation.success()) {
                return RunResult.compilationFailed(compilation);
            }
            try (CompiledProgram program = compilation.program()) {
                return RunResult.executed(compilation, executor.execute(program, stdin));
            }
        });
    }

    /**
     * Компилирует код один раз и запускает его на каждом из входов. Если код не скомпилировался,
     * {@code onCompiled} не вызывается и возвращается {@code onCompilationError}.
     */
    public <T> T runEach(String source, List<String> inputs,
                         Function<CompilationResult, T> onCompilationError,
                         Function<List<RunResult>, T> onCompiled) {
        return withSlot(() -> {
            CompilationResult compilation = compiler.compile(source);
            if (!compilation.success()) {
                return onCompilationError.apply(compilation);
            }
            try (CompiledProgram program = compilation.program()) {
                List<RunResult> results = inputs.stream()
                        .map(input -> RunResult.executed(compilation, executor.execute(program, input)))
                        .toList();
                return onCompiled.apply(results);
            }
        });
    }

    private <T> T withSlot(Supplier<T> action) {
        sandboxHealth.ensureVerified();
        boolean acquired;
        try {
            acquired = slots.tryAcquire(properties.queueTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RunnerBusyException();
        }
        if (!acquired) {
            throw new RunnerBusyException();
        }
        try {
            return action.get();
        } finally {
            slots.release();
        }
    }
}
