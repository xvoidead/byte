package dev.byteide.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

/**
 * Запускает скомпилированную программу в отдельной JVM с ограничениями по памяти, времени и объёму вывода.
 *
 * <p>Отдельный процесс защищает сервер от падений и бесконечных циклов, но не является полноценной
 * песочницей: в продакшене сервис нужно запускать в контейнере без сети и с непривилегированным пользователем
 * (см. Dockerfile).
 */
@Service
public class ProcessExecutionService {

    private final RunnerProperties properties;
    private final ExecutorService io = Executors.newVirtualThreadPerTaskExecutor();

    public ProcessExecutionService(RunnerProperties properties) {
        this.properties = properties;
    }

    public ExecutionResult execute(CompiledProgram program, String stdin) {
        long start = System.nanoTime();
        Process process;
        try {
            ProcessBuilder builder = new ProcessBuilder(command(program)).directory(program.classesDir().toFile());
            Map<String, String> env = builder.environment();
            env.clear();
            env.put("LANG", "C.UTF-8");
            process = builder.start();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось запустить JVM: " + e.getMessage(), e);
        }

        AtomicBoolean outputExceeded = new AtomicBoolean();
        BoundedCapture stdout = new BoundedCapture(properties.maxOutputBytes(), outputExceeded, process);
        BoundedCapture stderr = new BoundedCapture(properties.maxOutputBytes(), outputExceeded, process);
        Future<?> outReader = io.submit(() -> stdout.drain(process.getInputStream()));
        Future<?> errReader = io.submit(() -> stderr.drain(process.getErrorStream()));
        io.submit(() -> writeStdin(process, stdin));

        boolean finished;
        try {
            finished = process.waitFor(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finished = false;
        }
        if (!finished) {
            kill(process);
        }
        awaitQuietly(outReader);
        awaitQuietly(errReader);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        RunStatus status;
        Integer exitCode = null;
        if (outputExceeded.get()) {
            status = RunStatus.OUTPUT_LIMIT;
        } else if (!finished) {
            status = RunStatus.TIMEOUT;
        } else {
            exitCode = process.exitValue();
            status = exitCode == 0 ? RunStatus.SUCCESS : RunStatus.RUNTIME_ERROR;
        }
        return new ExecutionResult(status, stdout.text(), stderr.text(), exitCode, elapsedMs);
    }

    private List<String> command(CompiledProgram program) {
        return List.of(
                properties.javaExecutable().toString(),
                "-Xmx" + properties.maxMemoryMb() + "m",
                "-Xss16m",
                "-XX:+UseSerialGC",
                "-XX:TieredStopAtLevel=1",
                "-XX:ActiveProcessorCount=1",
                "-Xshare:auto",
                "-Djava.awt.headless=true",
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8",
                "-cp", ".",
                program.mainClass());
    }

    private static void writeStdin(Process process, String stdin) {
        try (OutputStream in = process.getOutputStream()) {
            if (stdin != null && !stdin.isEmpty()) {
                in.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
            // Программа могла завершиться, не дочитав ввод, — это нормально.
        }
    }

    private static void kill(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        try {
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void awaitQuietly(Future<?> future) {
        try {
            future.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(true);
        }
    }

    /** Читает поток целиком, но сохраняет не больше {@code limit} байт; при превышении убивает процесс. */
    private static final class BoundedCapture {
        private final int limit;
        private final AtomicBoolean exceeded;
        private final Process process;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        BoundedCapture(int limit, AtomicBoolean exceeded, Process process) {
            this.limit = limit;
            this.exceeded = exceeded;
            this.process = process;
        }

        void drain(InputStream stream) {
            byte[] chunk = new byte[8192];
            try (stream) {
                int read;
                while ((read = stream.read(chunk)) != -1) {
                    synchronized (buffer) {
                        int room = limit - buffer.size();
                        if (room > 0) {
                            buffer.write(chunk, 0, Math.min(room, read));
                        }
                        if (read > room) {
                            exceeded.set(true);
                            kill(process);
                            return;
                        }
                    }
                }
            } catch (IOException ignored) {
                // Поток закрылся вместе с процессом.
            }
        }

        String text() {
            synchronized (buffer) {
                byte[] bytes = buffer.toByteArray();
                return new String(bytes, 0, completeUtf8Length(bytes), StandardCharsets.UTF_8);
            }
        }

        /** Длина без незавершённого UTF-8 символа в конце, который мог появиться при обрезке вывода. */
        static int completeUtf8Length(byte[] bytes) {
            int end = bytes.length;
            for (int i = end - 1; i >= Math.max(0, end - 4); i--) {
                int b = bytes[i] & 0xFF;
                if ((b & 0xC0) != 0x80) {
                    int expected = b >= 0xF0 ? 4 : b >= 0xE0 ? 3 : b >= 0xC0 ? 2 : 1;
                    return end - i < expected ? i : end;
                }
            }
            return end;
        }
    }
}
