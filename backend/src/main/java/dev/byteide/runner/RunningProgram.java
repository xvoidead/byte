package dev.byteide.runner;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import dev.byteide.sandbox.SandboxLauncher;

/**
 * Запущенная программа ученика: потоковый вывод, ввод по ходу выполнения и контроль ограничений.
 *
 * <p>Сторожевой поток следит за временем и процессорным временем. Вывод отдаётся слушателю по мере появления;
 * всё, что JVM пишет в stderr до метки готовности песочницы (предупреждения о SecurityManager), отбрасывается.
 */
public final class RunningProgram {

    public enum Stream { STDOUT, STDERR }

    /** Получает вывод и итог. Вызывается из служебных потоков. */
    public interface Listener {
        Listener NONE = new Listener() {
        };

        default void onOutput(Stream stream, String text) {
        }

        default void onExit(ExecutionResult result) {
        }
    }

    public record Limits(Duration wallTimeout, Duration cpuLimit, int maxOutputChars) {
    }

    private static final Duration WATCH_INTERVAL = Duration.ofMillis(50);

    private final Process process;
    private final Limits limits;
    private final Listener listener;
    private final long startNanos = System.nanoTime();
    private final AtomicReference<RunStatus> killReason = new AtomicReference<>();
    private final AtomicInteger outputChars = new AtomicInteger();
    private final StringBuilder stdout = new StringBuilder();
    private final StringBuilder stderr = new StringBuilder();
    private final CompletableFuture<ExecutionResult> result = new CompletableFuture<>();
    private final MarkerFilter stderrFilter = new MarkerFilter();
    private final OutputStream stdin;

    private RunningProgram(Process process, Limits limits, Listener listener) {
        this.process = process;
        this.limits = limits;
        this.listener = listener;
        this.stdin = process.getOutputStream();
    }

    static RunningProgram start(ProcessBuilder builder, Limits limits, Listener listener, ExecutorService threads,
                                Runnable cleanup) {
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            cleanup.run();
            throw new IllegalStateException("Не удалось запустить JVM: " + e.getMessage(), e);
        }
        RunningProgram program = new RunningProgram(process, limits, listener);
        Future<?> out = threads.submit(() -> program.pump(Stream.STDOUT));
        Future<?> err = threads.submit(() -> program.pump(Stream.STDERR));
        threads.submit(program::watch);
        process.onExit().thenRunAsync(() -> program.finish(out, err, cleanup), threads);
        return program;
    }

    /** Передаёт программе текст так, будто его набрали с клавиатуры. */
    public synchronized void write(String text) {
        try {
            stdin.write(text.getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        } catch (IOException ignored) {
            // Программа уже завершилась или закрыла ввод.
        }
    }

    /** Конец ввода (Ctrl+D): Scanner.hasNext() вернёт false. */
    public synchronized void closeInput() {
        try {
            stdin.close();
        } catch (IOException ignored) {
            // уже закрыт
        }
    }

    /** Остановка по просьбе ученика. */
    public void stop() {
        kill(RunStatus.STOPPED);
    }

    public CompletableFuture<ExecutionResult> result() {
        return result;
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    /** true, если лаунчер успел включить защиту; иначе программа ученика не выполнялась. */
    boolean sandboxReady() {
        return stderrFilter.markerSeen();
    }

    private void pump(Stream stream) {
        char[] buffer = new char[4096];
        try (Reader reader = new InputStreamReader(
                stream == Stream.STDOUT ? process.getInputStream() : process.getErrorStream(), StandardCharsets.UTF_8)) {
            int read;
            while ((read = reader.read(buffer)) != -1) {
                String text = new String(buffer, 0, read);
                if (stream == Stream.STDERR) {
                    text = stderrFilter.accept(text);
                }
                deliver(stream, text);
            }
        } catch (IOException ignored) {
            // Поток закрылся вместе с процессом.
        }
    }

    private void deliver(Stream stream, String text) {
        if (text.isEmpty()) {
            return;
        }
        int before = outputChars.getAndAdd(text.length());
        int room = limits.maxOutputChars() - before;
        if (room <= 0) {
            kill(RunStatus.OUTPUT_LIMIT);
            return;
        }
        if (text.length() > room) {
            text = text.substring(0, room);
            kill(RunStatus.OUTPUT_LIMIT);
        }
        StringBuilder target = stream == Stream.STDOUT ? stdout : stderr;
        synchronized (target) {
            target.append(text);
        }
        listener.onOutput(stream, text);
    }

    private void watch() {
        try {
            while (process.isAlive()) {
                if (elapsed().compareTo(limits.wallTimeout()) > 0) {
                    kill(RunStatus.TIMEOUT);
                    return;
                }
                Duration cpu = process.toHandle().info().totalCpuDuration().orElse(Duration.ZERO);
                if (cpu.compareTo(limits.cpuLimit()) > 0) {
                    kill(RunStatus.TIMEOUT);
                    return;
                }
                Thread.sleep(WATCH_INTERVAL);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            kill(RunStatus.TIMEOUT);
        }
    }

    private void kill(RunStatus reason) {
        killReason.compareAndSet(null, reason);
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private void finish(Future<?> out, Future<?> err, Runnable cleanup) {
        try {
            awaitQuietly(out);
            awaitQuietly(err);
            String leftover = stderrFilter.flushIfNeverPassed();
            if (!leftover.isEmpty()) {
                synchronized (stderr) {
                    stderr.append(leftover);
                }
            }
            RunStatus reason = killReason.get();
            Integer exitCode = reason == null ? process.exitValue() : null;
            RunStatus status = reason != null ? reason
                    : exitCode == 0 ? RunStatus.SUCCESS : RunStatus.RUNTIME_ERROR;
            ExecutionResult execution = new ExecutionResult(status, text(stdout), text(stderr), exitCode,
                    elapsed().toMillis());
            try {
                listener.onExit(execution);
            } finally {
                result.complete(execution);
            }
        } catch (RuntimeException e) {
            result.completeExceptionally(e);
        } finally {
            cleanup.run();
        }
    }

    private Duration elapsed() {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }

    private static String text(StringBuilder sb) {
        synchronized (sb) {
            return sb.toString();
        }
    }

    private static void awaitQuietly(Future<?> future) {
        try {
            future.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(true);
        }
    }

    /** Отбрасывает всё до метки готовности песочницы включительно. */
    static final class MarkerFilter {
        private static final int MAX_PENDING = 16_384;
        private StringBuilder pending = new StringBuilder();
        /** Лаунчер напечатал метку — песочница включена. */
        private volatile boolean markerSeen;
        /** Дальнейший вывод передаётся без фильтрации. */
        private boolean released;

        synchronized String accept(String chunk) {
            if (released) {
                return chunk;
            }
            pending.append(chunk);
            int index = pending.indexOf(SandboxLauncher.READY_MARKER);
            if (index >= 0) {
                markerSeen = true;
                released = true;
                int from = index + SandboxLauncher.READY_MARKER.length();
                while (from < pending.length() && (pending.charAt(from) == '\r' || pending.charAt(from) == '\n')) {
                    from++;
                }
                String rest = pending.substring(from);
                pending = null;
                return rest;
            }
            if (pending.length() > MAX_PENDING) {
                // Метки нет, а stderr растёт — показываем как есть, но песочница не считается включённой.
                String all = pending.toString();
                pending = null;
                released = true;
                return all;
            }
            return "";
        }

        boolean markerSeen() {
            return markerSeen;
        }

        synchronized String flushIfNeverPassed() {
            if (released || pending == null) {
                return "";
            }
            String all = pending.toString();
            pending = null;
            released = true;
            return all;
        }
    }
}
