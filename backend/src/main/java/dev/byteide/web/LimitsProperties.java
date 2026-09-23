package dev.byteide.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Ограничения на запросы с одного IP-адреса.
 *
 * @param runsPerMinute         запусков и проверок кода в минуту
 * @param runBurst              сколько запусков можно сделать подряд, прежде чем включится ограничение
 * @param compilesPerMinute     проверок на ошибки во время набора в минуту
 * @param eventsPerMinute       пакетов аналитики в минуту
 * @param concurrentRunsPerIp   сколько программ один адрес может выполнять одновременно
 * @param maxRequestBytes       максимальный размер тела запроса
 */
@ConfigurationProperties("byte.limits")
public record LimitsProperties(
        @DefaultValue("30") int runsPerMinute,
        @DefaultValue("15") int runBurst,
        @DefaultValue("150") int compilesPerMinute,
        @DefaultValue("60") int eventsPerMinute,
        @DefaultValue("2") int concurrentRunsPerIp,
        @DefaultValue("524288") int maxRequestBytes) {
}
