package dev.byteide.runner;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Ограничения для запуска пользовательского кода.
 *
 * @param timeout            максимальное время работы программы (включая старт JVM)
 * @param maxMemoryMb        размер кучи дочерней JVM
 * @param maxOutputBytes     сколько байт stdout/stderr сохраняем, после чего программа останавливается
 * @param maxSourceLength    максимальная длина исходного кода в символах
 * @param maxStdinLength     максимальная длина stdin в символах
 * @param maxConcurrentRuns  сколько программ может выполняться одновременно
 * @param queueTimeout       сколько запрос ждёт свободного слота, прежде чем получить отказ
 * @param javaHome           JDK/JRE, которым запускаются программы; по умолчанию — текущий
 */
@ConfigurationProperties("byte.runner")
public record RunnerProperties(
        @DefaultValue("5s") Duration timeout,
        @DefaultValue("128") int maxMemoryMb,
        @DefaultValue("65536") int maxOutputBytes,
        @DefaultValue("50000") int maxSourceLength,
        @DefaultValue("100000") int maxStdinLength,
        @DefaultValue("4") int maxConcurrentRuns,
        @DefaultValue("10s") Duration queueTimeout,
        Path javaHome) {

    public Path javaExecutable() {
        Path home = javaHome != null ? javaHome : Path.of(System.getProperty("java.home"));
        return home.resolve("bin").resolve("java");
    }
}
