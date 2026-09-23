package dev.byteide.runner;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import dev.byteide.sandbox.SandboxLauncher;
import jakarta.annotation.PreDestroy;

/**
 * Готовит окружение песочницы: каталог с классами лаунчера и файл политики безопасности,
 * и собирает команду запуска дочерней JVM.
 *
 * <p>Уровни защиты:
 * <ul>
 *   <li>SecurityManager с пустой политикой для кода ученика: нет файлов, сети, процессов,
 *       переменных окружения, рефлексии и управления другими процессами;</li>
 *   <li>ограничения JVM: куча, метапространство, кеш кода, direct-память, число потоков;</li>
 *   <li>сервер следит за временем и процессорным временем и убивает процесс при превышении.</li>
 * </ul>
 * SecurityManager доступен только до Java 23 включительно; на более новой JVM лаунчер отказывается
 * запускать код, а самопроверка при старте выключает выполнение программ.
 */
@Component
public class Sandbox {

    private final RunnerProperties properties;
    private final Path home;
    private final Path launcherDir;
    private final Path policyFile;

    public Sandbox(RunnerProperties properties) {
        this.properties = properties;
        try {
            this.home = Files.createTempDirectory("byte-sandbox-");
            this.launcherDir = Files.createDirectories(home.resolve("launcher"));
            copyLauncherClasses(launcherDir);
            this.policyFile = home.resolve("sandbox.policy");
            Files.writeString(policyFile, policy(launcherDir), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось подготовить песочницу", e);
        }
    }

    /** Команда запуска программы ученика; рабочий каталог — каталог с её классами. */
    public List<String> command(CompiledProgram program) {
        List<String> command = new ArrayList<>(List.of(
                properties.javaExecutable().toString(),
                "-Xmx" + properties.maxMemoryMb() + "m",
                "-Xss8m",
                "-XX:MaxMetaspaceSize=64m",
                "-XX:ReservedCodeCacheSize=32m",
                "-XX:MaxDirectMemorySize=16m",
                "-XX:+UseSerialGC",
                "-XX:TieredStopAtLevel=1",
                "-XX:ActiveProcessorCount=1",
                "-XX:-UsePerfData",
                "-XX:+DisableAttachMechanism",
                "-Xshare:auto",
                "-Djava.awt.headless=true",
                "-Djava.io.tmpdir=" + program.classesDir(),
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8",
                "-Djava.security.manager=allow",
                "-Djava.security.policy==" + policyFile,
                "-cp", launcherDir.toString(),
                SandboxLauncher.class.getName(),
                program.mainClass(),
                program.classesDir().toString(),
                String.valueOf(properties.maxThreads())));
        return command;
    }

    @PreDestroy
    void cleanUp() {
        new CompiledProgram(home, "").close();
    }

    private static void copyLauncherClasses(Path target) throws IOException {
        for (Class<?> cls : SandboxLauncher.classesToCopy()) {
            String resource = cls.getName().replace('.', '/') + ".class";
            Path file = target.resolve(resource);
            Files.createDirectories(file.getParent());
            try (InputStream in = cls.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IOException("Не найден класс лаунчера " + resource);
                }
                Files.copy(in, file);
            }
        }
    }

    private static String policy(Path launcherDir) {
        String codeBase = "file:" + launcherDir.toAbsolutePath() + "/";
        return """
                // Лаунчер песочницы: полные права.
                grant codeBase "%s-" {
                    permission java.security.AllPermission;
                };

                // Код ученика: только завершение программы и чтение безобидных свойств.
                grant {
                    permission java.lang.RuntimePermission "exitVM.*";
                    permission java.util.PropertyPermission "line.separator", "read";
                    permission java.util.PropertyPermission "file.separator", "read";
                    permission java.util.PropertyPermission "path.separator", "read";
                    permission java.util.PropertyPermission "java.version", "read";
                    permission java.util.PropertyPermission "java.vendor", "read";
                    permission java.util.PropertyPermission "java.specification.version", "read";
                    permission java.util.PropertyPermission "os.name", "read";
                    permission java.util.PropertyPermission "os.arch", "read";
                };
                """.formatted(codeBase);
    }
}
