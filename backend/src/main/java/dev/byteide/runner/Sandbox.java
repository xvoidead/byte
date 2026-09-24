package dev.byteide.runner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.byteide.sandbox.SandboxLauncher;
import jakarta.annotation.PreDestroy;

/**
 * Готовит окружение песочницы: модуль лаунчера и файл политики безопасности,
 * и собирает команду запуска дочерней JVM.
 *
 * <p>Уровни защиты:
 * <ul>
 *   <li>SecurityManager с почти пустой политикой для кода ученика: нет сети, процессов, переменных окружения,
 *       управления другими процессами; файлы — только в рабочей папке этого запуска;</li>
 *   <li>лаунчер — отдельный модуль, который ничего не экспортирует и не открывает: даже с рефлексией
 *       (она нужна Gson и SnakeYAML) код ученика не доберётся до внутренностей защиты. Из модулей JDK
 *       подключается только Java SE, без {@code jdk.unsupported} с его {@code sun.misc.Unsafe};</li>
 *   <li>ограничения JVM: куча, метапространство, кеш кода, direct-память, число потоков;
 *       ограничение ОС на размер файла ({@code ulimit -f});</li>
 *   <li>сервер следит за временем, процессорным временем и размером рабочей папки
 *       и убивает процесс при превышении.</li>
 * </ul>
 * SecurityManager доступен только до Java 23 включительно; на более новой JVM лаунчер отказывается
 * запускать код, а самопроверка при старте выключает выполнение программ.
 */
@Component
public class Sandbox {

    static final String MODULE = "byteide.sandbox";
    private static final Path SHELL = Path.of("/bin/sh");

    private final RunnerProperties properties;
    private final StudentLibraries libraries;
    private final Path home;
    private final Path launcherDir;
    private final Path policyFile;

    public Sandbox(RunnerProperties properties) {
        this(properties, StudentLibraries.shared());
    }

    @Autowired
    public Sandbox(RunnerProperties properties, StudentLibraries libraries) {
        this.properties = properties;
        this.libraries = libraries;
        try {
            this.home = Files.createTempDirectory("byte-sandbox-");
            this.launcherDir = Files.createDirectories(home.resolve("launcher"));
            copyLauncherClasses(launcherDir);
            compileModuleDescriptor(home, launcherDir);
            this.policyFile = home.resolve("sandbox.policy");
            Files.writeString(policyFile, policy(launcherDir), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось подготовить песочницу", e);
        }
    }

    /** Команда запуска программы ученика; рабочий каталог процесса — {@code workDir}. */
    public List<String> command(CompiledProgram program, Path workDir) {
        List<String> command = new ArrayList<>();
        if (Files.isExecutable(SHELL)) {
            // Размер файла, который может записать программа. /bin/sh считает в блоках по 512 байт.
            long blocks = properties.maxFileSizeKb() * 2L;
            command.addAll(List.of(SHELL.toString(), "-c", "ulimit -f " + blocks + " && exec \"$0\" \"$@\""));
        }
        command.addAll(List.of(
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
                "-Djava.io.tmpdir=" + workDir,
                "-Dbyte.workdir=" + workDir,
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8",
                "-Djava.security.manager=allow",
                "-Djava.security.policy==" + policyFile,
                "--module-path", launcherDir.toString(),
                "--add-modules", "java.se",
                "-m", MODULE + "/" + SandboxLauncher.class.getName(),
                program.mainClass(),
                program.classesDir().toString(),
                String.valueOf(properties.maxThreads()),
                libraries.jars().stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator))));
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

    /**
     * Описание модуля лаунчера: модуль ничего не экспортирует и не открывает. Компилируется при старте,
     * потому что сам сервер не модульный.
     */
    private static void compileModuleDescriptor(Path home, Path launcherDir) throws IOException {
        Path source = home.resolve("module-info.java");
        Files.writeString(source, "module " + MODULE + " {\n}\n", StandardCharsets.UTF_8);
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        int code = javac.run(null, null, null, "-d", launcherDir.toString(), source.toString());
        if (code != 0) {
            throw new IOException("Не удалось скомпилировать module-info лаунчера");
        }
    }

    private static String policy(Path launcherDir) {
        String codeBase = "file:" + launcherDir.toAbsolutePath() + "/";
        return """
                // Лаунчер песочницы: полные права.
                grant codeBase "%s-" {
                    permission java.security.AllPermission;
                };

                // Код ученика и библиотеки (Gson, SnakeYAML).
                grant {
                    permission java.lang.RuntimePermission "exitVM.*";

                    // Файлы — только в рабочей папке этого запуска.
                    permission java.io.FilePermission "${byte.workdir}", "read";
                    permission java.io.FilePermission "${byte.workdir}${/}-", "read,write,delete";

                    // Рефлексия внутри своего кода: без неё не работают Gson и SnakeYAML. Классы JDK и лаунчера
                    // защищены модулями: они не открыты для кода ученика.
                    permission java.lang.reflect.ReflectPermission "suppressAccessChecks";
                    permission java.lang.RuntimePermission "accessDeclaredMembers";

                    permission java.util.PropertyPermission "line.separator", "read";
                    permission java.util.PropertyPermission "file.separator", "read";
                    permission java.util.PropertyPermission "path.separator", "read";
                    permission java.util.PropertyPermission "user.dir", "read";
                    permission java.util.PropertyPermission "java.version", "read";
                    permission java.util.PropertyPermission "java.vendor", "read";
                    permission java.util.PropertyPermission "java.specification.*", "read";
                    permission java.util.PropertyPermission "java.runtime.*", "read";
                    permission java.util.PropertyPermission "java.vm.*", "read";
                    permission java.util.PropertyPermission "os.name", "read";
                    permission java.util.PropertyPermission "os.arch", "read";
                    // Настройки самих библиотек (например, TypeToken в Gson читает gson.allowCapturingTypeVariables).
                    permission java.util.PropertyPermission "gson.*", "read";
                };
                """.formatted(codeBase);
    }
}
