package dev.byteide.runner;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

/**
 * Запускает скомпилированную программу в песочнице — отдельной JVM с SecurityManager
 * и ограничениями по памяти, потокам, времени, процессорному времени и объёму вывода.
 * Программам с PostgreSQL внутри (PGlite) даётся больше памяти и времени, но работает их одновременно немного.
 */
@Service
public class ProcessExecutionService {

    private final RunnerProperties properties;
    private final Sandbox sandbox;
    private final ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor();
    /** Тяжёлые программы (с PostgreSQL внутри) занимают сотни мегабайт — их одновременно запускается немного. */
    private final Semaphore heavySlots;

    public ProcessExecutionService(RunnerProperties properties, Sandbox sandbox) {
        this.properties = properties;
        this.sandbox = sandbox;
        this.heavySlots = new Semaphore(properties.maxConcurrentHeavyRuns(), true);
    }

    /** Обычный запуск: весь ввод передаётся сразу, результат — когда программа завершится. */
    public ExecutionResult execute(CompiledProgram program, String stdin) {
        return execute(program, ExecutionInput.of(stdin));
    }

    public ExecutionResult execute(CompiledProgram program, ExecutionInput input) {
        if (program.heavy()) {
            acquireHeavySlot(properties.queueTimeout());
        }
        try {
            Duration timeout = program.heavy() ? properties.heavyTimeout() : properties.timeout();
            WorkDir workDir = WorkDir.create(input.workFiles());
            RunningProgram running = start(program, workDir, timeout, RunningProgram.Listener.NONE, workDir::close);
            threads.submit(() -> {
                if (!input.stdin().isEmpty()) {
                    running.write(input.stdin());
                }
                running.closeInput();
            });
            ExecutionResult result = await(running, timeout.plusSeconds(10));
            if (!running.sandboxReady() && result.exitCode() != null) {
                throw new SandboxUnavailableException(result.stderr().strip());
            }
            return result;
        } finally {
            if (program.heavy()) {
                heavySlots.release();
            }
        }
    }

    /**
     * Интерактивный запуск: вывод приходит слушателю по мере появления, ввод передаётся через
     * {@link RunningProgram#write}. Каталоги программы удаляются после её завершения.
     */
    public RunningProgram startInteractive(CompiledProgram program, List<ProjectFile> workFiles,
                                           RunningProgram.Listener listener) {
        if (program.heavy()) {
            acquireHeavySlot(Duration.ZERO);
        }
        boolean started = false;
        try {
            WorkDir workDir = WorkDir.create(workFiles);
            RunningProgram running = start(program, workDir, properties.interactiveTimeout(), listener, () -> {
                workDir.close();
                program.close();
                if (program.heavy()) {
                    heavySlots.release();
                }
            });
            started = true;
            return running;
        } finally {
            if (!started && program.heavy()) {
                heavySlots.release();
            }
        }
    }

    private void acquireHeavySlot(Duration wait) {
        try {
            if (!heavySlots.tryAcquire(wait.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new RunnerBusyException();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RunnerBusyException();
        }
    }

    private RunningProgram start(CompiledProgram program, WorkDir workDir, Duration wallTimeout,
                                 RunningProgram.Listener listener, Runnable cleanup) {
        ProcessBuilder builder = new ProcessBuilder(sandbox.command(program, workDir.path()))
                .directory(workDir.path().toFile());
        Map<String, String> env = builder.environment();
        env.clear();
        env.put("LANG", "C.UTF-8");
        Duration cpuLimit = program.heavy() ? properties.heavyCpuLimit() : properties.cpuLimit();
        RunningProgram.Limits limits = new RunningProgram.Limits(wallTimeout, cpuLimit,
                properties.maxOutputChars(), properties.maxWorkDirSizeKb() * 1024L, properties.maxWorkDirEntries());
        return RunningProgram.start(builder, workDir, limits, listener, threads, cleanup);
    }

    private static ExecutionResult await(RunningProgram running, Duration timeout) {
        try {
            return running.result().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running.stop();
            throw new IllegalStateException("Выполнение прервано", e);
        } catch (ExecutionException | TimeoutException e) {
            running.stop();
            throw new IllegalStateException("Не удалось дождаться завершения программы", e);
        }
    }
}
