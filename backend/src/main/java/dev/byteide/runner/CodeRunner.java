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
        return startInteractive(Project.single(source), listener);
    }

    public Interactive startInteractive(Project project, RunningProgram.Listener listener) {
        sandboxHealth.ensureVerified();
        if (!interactiveSlots.tryAcquire()) {
            throw new RunnerBusyException();
        }
        boolean started = false;
        try {
            CompilationResult compilation = compileWithSlot(project);
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
            RunningProgram program = executor.startInteractive(compilation.program(), project.workFiles(), releasing);
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
        return diagnose(Project.single(source));
    }

    public JavaCompilationService.DiagnosticsResult diagnose(Project project) {
        acquire(compileSlots, 2_000);
        try {
            return compiler.diagnose(project);
        } finally {
            compileSlots.release();
        }
    }

    private CompilationResult compileWithSlot(Project project) {
        acquire(compileSlots, properties.queueTimeout().toMillis());
        try {
            return compiler.compile(project);
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
        return run(Project.single(source), stdin);
    }

    /** Обычный запуск: файлы проекта (кроме исходников и ресурсов) кладутся в рабочую папку программы. */
    public RunResult run(Project project, String stdin) {
        return withSlot(() -> {
            CompilationResult compilation = compiler.compile(project);
            if (!compilation.success()) {
                return RunResult.compilationFailed(compilation);
            }
            try (CompiledProgram program = compilation.program()) {
                ExecutionInput input = new ExecutionInput(stdin, project.workFiles());
                return RunResult.executed(compilation, executor.execute(program, input));
            }
        });
    }

    /**
     * Компилирует проект один раз и запускает его на каждом из входов. Если код не скомпилировался,
     * {@code onCompiled} не вызывается и возвращается {@code onCompilationError}.
     */
    public <T> T runEach(Project project, List<ExecutionInput> inputs,
                         Function<CompilationResult, T> onCompilationError,
                         Function<List<RunResult>, T> onCompiled) {
        return withSlot(() -> {
            CompilationResult compilation = compiler.compile(project);
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

    public <T> T runEach(String source, List<String> inputs,
                         Function<CompilationResult, T> onCompilationError,
                         Function<List<RunResult>, T> onCompiled) {
        return runEach(Project.single(source), inputs.stream().map(ExecutionInput::of).toList(),
                onCompilationError, onCompiled);
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
