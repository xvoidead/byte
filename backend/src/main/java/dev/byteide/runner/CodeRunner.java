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

    public CodeRunner(JavaCompilationService compiler, ProcessExecutionService executor, SandboxHealth sandboxHealth,
                      RunnerProperties properties) {
        this.compiler = compiler;
        this.executor = executor;
        this.sandboxHealth = sandboxHealth;
        this.properties = properties;
        this.slots = new Semaphore(properties.maxConcurrentRuns(), true);
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
