package dev.byteide.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Настройки анонимной аналитики.
 *
 * @param adminToken    токен для страницы /stats; если не задан, статистика недоступна
 * @param retentionDays сколько дней хранить события
 */
@ConfigurationProperties("byte.analytics")
public record AnalyticsProperties(String adminToken, @DefaultValue("180") int retentionDays) {
}
