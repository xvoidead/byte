package dev.byteide.runner;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Ограничения для запуска пользовательского кода.
 *
 * @param timeout                максимальное время работы программы при обычном запуске и проверке (включая старт JVM)
 * @param cpuLimit               максимальное процессорное время программы
 * @param interactiveTimeout     сколько может жить программа в интерактивной консоли (она ждёт ввода от человека)
 * @param maxMemoryMb            размер кучи дочерней JVM
 * @param maxThreads             сколько потоков может создать программа
 * @param maxOutputChars         сколько символов stdout/stderr сохраняем, после чего программа останавливается
 * @param maxSourceLength        максимальная длина исходного кода в символах
 * @param maxStdinLength         максимальная длина stdin в символах
 * @param maxConcurrentRuns      сколько программ может выполняться одновременно при обычном запуске и проверке
 * @param maxInteractiveSessions сколько программ может одновременно работать в интерактивной консоли
 * @param maxConcurrentCompiles  сколько проверок кода на ошибки (во время набора) может идти одновременно
 * @param queueTimeout           сколько запрос ждёт свободного слота, прежде чем получить отказ
 * @param maxFileSizeKb          максимальный размер одного файла, который программа может записать
 * @param maxWorkDirSizeKb       сколько всего данных может лежать в рабочей папке программы
 * @param maxWorkDirEntries      сколько файлов и папок может быть в рабочей папке
 * @param javaHome               JDK/JRE, которым запускаются программы; по умолчанию — текущий
 * @param heavyMemoryMb          размер кучи «тяжёлой» программы — той, что запускает PostgreSQL внутри себя
 *                               (PGlite — настоящий PostgreSQL в WebAssembly, ему нужно около 600 МБ)
 * @param heavyTimeout           максимальное время работы тяжёлой программы при обычном запуске и проверке
 * @param heavyCpuLimit          максимальное процессорное время тяжёлой программы
 * @param maxConcurrentHeavyRuns сколько тяжёлых программ может работать одновременно (вместе с консолью)
 */
@ConfigurationProperties("byte.runner")
public record RunnerProperties(
        @DefaultValue("5s") Duration timeout,
        @DefaultValue("5s") Duration cpuLimit,
        @DefaultValue("2m") Duration interactiveTimeout,
        @DefaultValue("128") int maxMemoryMb,
        @DefaultValue("32") int maxThreads,
        @DefaultValue("65536") int maxOutputChars,
        @DefaultValue("50000") int maxSourceLength,
        @DefaultValue("100000") int maxStdinLength,
        @DefaultValue("4") int maxConcurrentRuns,
        @DefaultValue("8") int maxInteractiveSessions,
        @DefaultValue("4") int maxConcurrentCompiles,
        @DefaultValue("10s") Duration queueTimeout,
        @DefaultValue("2048") int maxFileSizeKb,
        @DefaultValue("4096") int maxWorkDirSizeKb,
        @DefaultValue("100") int maxWorkDirEntries,
        Path javaHome,
        @DefaultValue("640") int heavyMemoryMb,
        @DefaultValue("15s") Duration heavyTimeout,
        @DefaultValue("15s") Duration heavyCpuLimit,
        @DefaultValue("1") int maxConcurrentHeavyRuns) {

    public Path javaExecutable() {
        Path home = javaHome != null ? javaHome : Path.of(System.getProperty("java.home"));
        return home.resolve("bin").resolve("java");
    }

    /** Настройки по умолчанию — для тестов и инструментов вне Spring. */
    public static RunnerProperties defaults() {
        return new RunnerProperties(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofMinutes(2), 128, 32,
                65536, 50_000, 100_000, 4, 8, 4, Duration.ofSeconds(10), 2048, 4096, 100, null,
                640, Duration.ofSeconds(15), Duration.ofSeconds(15), 1);
    }
}
