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
 *       управления другими процессами; файлы — только в рабочей папке этого запуска. Базы данных (SQLite,
 *       PostgreSQL, MongoDB) работают внутри самой программы, без сети; права сверх этого есть только
 *       у библиотек MockBukkit и только на время запуска сервера и загрузки плагина;</li>
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
            Files.writeString(policyFile, policy(launcherDir, libraries), StandardCharsets.UTF_8);
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
        // PGlite растит память WebAssembly копированием массива, и G1 справляется с этим в куче вдвое меньшей,
        // чем последовательный сборщик.
        boolean heavy = program.heavy();
        command.addAll(List.of(
                properties.javaExecutable().toString(),
                "-Xmx" + (heavy ? properties.heavyMemoryMb() : properties.maxMemoryMb()) + "m",
                "-Xss8m",
                "-XX:MaxMetaspaceSize=" + (heavy ? 128 : 64) + "m",
                "-XX:ReservedCodeCacheSize=32m",
                "-XX:MaxDirectMemorySize=16m",
                heavy ? "-XX:+UseG1GC" : "-XX:+UseSerialGC",
                "-XX:TieredStopAtLevel=1",
                "-XX:ActiveProcessorCount=1",
                "-XX:-UsePerfData",
                "-XX:+DisableAttachMechanism",
                "-Xshare:auto",
                "-Djava.awt.headless=true",
                "-Djava.io.tmpdir=" + toPolicyPath(workDir),
                "-Dbyte.workdir=" + toPolicyPath(workDir),
                "-Dbyte.classes=" + toPolicyPath(program.classesDir()),
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8",
                "-Djava.security.manager=allow",
                "-Djava.security.policy==" + policyFile.toUri().toASCIIString(),
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

    /**
     * Библиотеки, которым нужны права сверх прав ученика, и сами эти права. MockBukkit и его зависимости создают
     * загрузчик классов для плагина — ученику это запрещено: подкласс ClassLoader мог бы выдать своим классам
     * любые права. Эти права действуют, только когда в стеке нет кода ученика: MockBukkit получает их через
     * ограниченный doPrivileged (см. src/bridge/.../ByteSandbox.java), и кадры плагина выше в стеке всё равно
     * проверяются.
     */
    private static final List<String> PLUGIN_LOADER_PERMISSIONS = List.of(
            "java.lang.RuntimePermission \"createClassLoader\"",
            "java.lang.RuntimePermission \"getClassLoader\"",
            "java.lang.RuntimePermission \"getProtectionDomain\"",
            "java.lang.RuntimePermission \"net.bytebuddy.createJavaDispatcher\"",
            "java.lang.reflect.ReflectPermission \"newProxyInPackage.net.bytebuddy.*\"",
            "java.util.PropertyPermission \"net.bytebuddy.*\", \"read\"");

    private static String policy(Path launcherDir, StudentLibraries libraries) {
        String codeBase = toPolicyCodeBase(launcherDir);
        StringBuilder libraryGrants = new StringBuilder();
        for (Path path : libraries.jars()) {
            String name = path.getFileName().toString();
            if (name.equals(StudentLibraries.BRIDGE_DIR) || name.startsWith("mockbukkit-")
                    || name.startsWith("paper-api-") || name.startsWith("byte-buddy-")) {
                grant(libraryGrants, path, PLUGIN_LOADER_PERMISSIONS);
            } else if (name.startsWith("netty-common-")) {
                // Netty узнаёт системный загрузчик внутри своего doPrivileged, когда оценивает доступную память.
                grant(libraryGrants, path, List.of("java.lang.RuntimePermission \"getClassLoader\""));
            }
        }
        return """
                // Лаунчер песочницы: полные права.
                grant codeBase "%s" {
                    permission java.security.AllPermission;
                };

                // Код ученика и библиотеки.
                grant {
                    permission java.lang.RuntimePermission "exitVM.*";

                    // Файлы — только в рабочей папке этого запуска. Запись в саму папку нужна SQLite.
                    permission java.io.FilePermission "${byte.workdir}", "read,write";
                    permission java.io.FilePermission "${byte.workdir}${/}-", "read,write,delete";
                    // Чтение своих классов и ресурсов (plugin.yml, config.yml): свой каталог код ученика читать может
                    // и так, а MockBukkit ищет в нём plugin.yml из своего кода.
                    permission java.io.FilePermission "${byte.classes}${/}-", "read";
                    // Сами библиотеки: MockBukkit читает данные из своего jar.
                    permission java.io.FilePermission "%s${/}-", "read";

                    // Рефлексия внутри своего кода: без неё не работают Gson, SnakeYAML, MockBukkit. Классы JDK
                    // и лаунчера защищены модулями: они не открыты для кода ученика.
                    permission java.lang.reflect.ReflectPermission "suppressAccessChecks";
                    permission java.lang.RuntimePermission "accessDeclaredMembers";
                    // HandlerList в Bukkit находит класс события по стеку вызовов.
                    permission java.lang.RuntimePermission "getStackWalkerWithClassReference";
                    // Потоки и журнал — только внутри своей JVM: драйвер MongoDB останавливает свои потоки при close(),
                    // MockBukkit настраивает java.util.logging.
                    permission java.lang.RuntimePermission "modifyThread";
                    permission java.util.logging.LoggingPermission "control";
                    // SQLite и PostgreSQL в WebAssembly держат файлы в своей файловой системе в памяти.
                    permission java.lang.RuntimePermission "fileSystemProvider";
                    permission java.lang.RuntimePermission "accessUserInformation";
                    // Драйвер PostgreSQL проверяет, что адрес localhost существует. Сети всё равно нет: соединение
                    // идёт в PostgreSQL внутри программы, а сокеты запрещены.
                    permission java.net.SocketPermission "localhost", "resolve";
                    // Драйвер MongoDB определяет облачную платформу по переменным окружения. Окружение программы
                    // пустое, но читать разрешено только эти имена.
                    permission java.lang.RuntimePermission "getenv.AWS_EXECUTION_ENV";
                    permission java.lang.RuntimePermission "getenv.AWS_LAMBDA_RUNTIME_API";
                    permission java.lang.RuntimePermission "getenv.FUNCTION_NAME";
                    permission java.lang.RuntimePermission "getenv.FUNCTIONS_WORKER_RUNTIME";
                    permission java.lang.RuntimePermission "getenv.K_SERVICE";
                    permission java.lang.RuntimePermission "getenv.KUBERNETES_SERVICE_HOST";
                    permission java.lang.RuntimePermission "getenv.VERCEL";

                    permission java.util.PropertyPermission "line.separator", "read";
                    permission java.util.PropertyPermission "file.separator", "read";
                    permission java.util.PropertyPermission "path.separator", "read";
                    permission java.util.PropertyPermission "user.dir", "read";
                    permission java.util.PropertyPermission "java.io.tmpdir", "read";
                    permission java.util.PropertyPermission "java.version", "read";
                    permission java.util.PropertyPermission "java.vendor", "read";
                    permission java.util.PropertyPermission "java.specification.*", "read";
                    permission java.util.PropertyPermission "java.runtime.*", "read";
                    permission java.util.PropertyPermission "java.vm.*", "read";
                    permission java.util.PropertyPermission "os.name", "read";
                    permission java.util.PropertyPermission "os.arch", "read";
                    permission java.util.PropertyPermission "sun.arch.data.model", "read";
                    permission java.util.PropertyPermission "socksProxyHost", "read";
                    // Настройки самих библиотек (например, TypeToken в Gson читает gson.allowCapturingTypeVariables).
                    permission java.util.PropertyPermission "gson.*", "read";
                    permission java.util.PropertyPermission "slf4j.*", "read";
                    permission java.util.PropertyPermission "io.netty.*", "read";
                    permission java.util.PropertyPermission "org.jboss.netty.*", "read";
                    permission java.util.PropertyPermission "jctools.*", "read";
                    permission java.util.PropertyPermission "sun.misc.unsafe.memory.access", "read";
                    permission java.util.PropertyPermission "org.graalvm.nativeimage.imagecode", "read";
                    permission java.util.PropertyPermission "net.kyori.*", "read";
                    permission java.util.PropertyPermission "postgresql.*", "read";
                    permission java.util.PropertyPermission "org.postgresql.*", "read";
                    permission java.util.PropertyPermission "pgjdbc.*", "read";
                    permission java.util.PropertyPermission "chicory.*", "read";
                    permission java.util.PropertyPermission "com.dylibso.chicory.*", "read";
                    permission java.util.PropertyPermission "endive.*", "read";
                    permission java.util.PropertyPermission "run.endive.*", "read";
                };
                %s""".formatted(codeBase, toPolicyPath(libraries.root()), libraryGrants);
    }

    private static String toPolicyCodeBase(Path path) {
        String uri = path.toAbsolutePath().normalize().toUri().toASCIIString();
        if (Files.isDirectory(path)) {
            if (!uri.endsWith("/")) {
                uri += "/";
            }
            return uri + "-";
        }
        return uri;
    }

    /** Policy uses URI-form code bases and slash-normalized paths in FilePermission entries. */
    private static String toPolicyPath(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    private static void grant(StringBuilder sb, Path path, List<String> permissions) {
        String codeBase = toPolicyCodeBase(path);
        sb.append("\ngrant codeBase \"").append(codeBase).append("\" {\n");
        for (String permission : permissions) {
            sb.append("    permission ").append(permission).append(";\n");
        }
        sb.append("};\n");
    }

}
