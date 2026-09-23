package dev.byteide.site;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки сайта.
 *
 * @param baseUrl      публичный адрес сайта, например https://byte.example — для sitemap и превью ссылок;
 *                     если не задан, берётся из запроса
 * @param contactEmail адрес для связи на страницах правил и конфиденциальности; если не задан, не показывается
 */
@ConfigurationProperties("byte.site")
public record SiteProperties(String baseUrl, String contactEmail) {
}
