package dev.byteide.runner;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

/**
 * Запускает скомпилированную программу в песочнице — отдельной JVM с SecurityManager
 * и ограничениями по памяти, потокам, времени, процессорному времени и объёму вывода.
 */
@Service
public class ProcessExecutionService {

    private final RunnerProperties properties;
    private final Sandbox sandbox;
    private final ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor();

    public ProcessExecutionService(RunnerProperties properties, Sandbox sandbox) {
        this.properties = properties;
        this.sandbox = sandbox;
    }

    /** Обычный запуск: весь ввод передаётся сразу, результат — когда программа завершится. */
    public ExecutionResult execute(CompiledProgram program, String stdin) {
        RunningProgram running = start(program, properties.timeout(), RunningProgram.Listener.NONE, () -> {
        });
        threads.submit(() -> {
            if (stdin != null && !stdin.isEmpty()) {
                running.write(stdin);
            }
            running.closeInput();
        });
        ExecutionResult result = await(running, properties.timeout().plusSeconds(10));
        if (!running.sandboxReady() && result.exitCode() != null) {
            throw new SandboxUnavailableException(result.stderr().strip());
        }
        return result;
    }

    /**
     * Интерактивный запуск: вывод приходит слушателю по мере появления, ввод передаётся через
     * {@link RunningProgram#write}. Каталог программы удаляется после её завершения.
     */
    public RunningProgram startInteractive(CompiledProgram program, RunningProgram.Listener listener) {
        return start(program, properties.interactiveTimeout(), listener, program::close);
    }

    private RunningProgram start(CompiledProgram program, Duration wallTimeout, RunningProgram.Listener listener,
                                 Runnable cleanup) {
        ProcessBuilder builder = new ProcessBuilder(sandbox.command(program)).directory(program.classesDir().toFile());
        Map<String, String> env = builder.environment();
        env.clear();
        env.put("LANG", "C.UTF-8");
        RunningProgram.Limits limits = new RunningProgram.Limits(wallTimeout, properties.cpuLimit(),
                properties.maxOutputChars());
        return RunningProgram.start(builder, limits, listener, threads, cleanup);
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
