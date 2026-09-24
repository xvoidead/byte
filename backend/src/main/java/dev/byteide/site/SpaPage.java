package dev.byteide.site;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * index.html фронтенда с мета-тегами конкретной страницы: заголовок, описание и Open Graph,
 * чтобы ссылка на урок в мессенджере показывала его название, а поисковик — описание.
 */
@Component
public class SpaPage {

    private static final Pattern SEO_BLOCK = Pattern.compile("<!-- seo -->.*?<!-- /seo -->", Pattern.DOTALL);
    private static final Pattern TITLE = Pattern.compile("<title>.*?</title>", Pattern.DOTALL);

    /** Используется, если сборки фронтенда нет рядом (например, в тестах). */
    private static final String FALLBACK = """
            <!doctype html>
            <html lang="ru">
              <head>
                <meta charset="UTF-8" />
                <!-- seo --><title>byte</title><!-- /seo -->
              </head>
              <body><div id="root"></div></body>
            </html>
            """;

    public static final String DEFAULT_TITLE = "byte — Java в браузере";
    public static final String DEFAULT_DESCRIPTION =
            "Изучайте Java с настоящей IDE прямо в браузере: короткие уроки, задания с автопроверкой, песочница и проекты с конфигами на YAML и JSON.";

    private final ResourceLoader resources;
    private final WebProperties web;
    private final SiteProperties site;

    public SpaPage(ResourceLoader resources, WebProperties web, SiteProperties site) {
        this.resources = resources;
        this.web = web;
        this.site = site;
    }

    public record Meta(String title, String description) {
    }

    public ResponseEntity<String> render(HttpStatus status, Meta meta) {
        String url = ServletUriComponentsBuilder.fromCurrentRequest().replaceQuery(null).toUriString();
        if (site.baseUrl() != null && !site.baseUrl().isBlank()) {
            String path = ServletUriComponentsBuilder.fromCurrentRequest().build().getPath();
            url = baseUrl() + (path == null ? "" : path);
        }
        String html = inject(template(), meta, url, baseUrl());
        return ResponseEntity.status(status)
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noCache())
                .body(html);
    }

    /** Публичный адрес сайта без завершающего слеша. */
    public String baseUrl() {
        if (site.baseUrl() != null && !site.baseUrl().isBlank()) {
            return site.baseUrl().replaceAll("/+$", "");
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }

    static String inject(String template, Meta meta, String url, String baseUrl) {
        String title = escape(meta.title());
        String description = escape(meta.description());
        String block = """
                <!-- seo -->
                    <title>%1$s</title>
                    <meta name="description" content="%2$s" />
                    <link rel="canonical" href="%3$s" />
                    <meta property="og:type" content="website" />
                    <meta property="og:site_name" content="byte" />
                    <meta property="og:locale" content="ru_RU" />
                    <meta property="og:title" content="%1$s" />
                    <meta property="og:description" content="%2$s" />
                    <meta property="og:url" content="%3$s" />
                    <meta property="og:image" content="%4$s/og-image.png" />
                    <meta property="og:image:width" content="1200" />
                    <meta property="og:image:height" content="630" />
                    <meta name="twitter:card" content="summary_large_image" />
                    <!-- /seo -->""".formatted(title, description, escape(url), escape(baseUrl));
        Matcher seo = SEO_BLOCK.matcher(template);
        if (seo.find()) {
            return seo.replaceFirst(Matcher.quoteReplacement(block));
        }
        return TITLE.matcher(template).replaceFirst(Matcher.quoteReplacement("<title>" + title + "</title>"));
    }

    private String template() {
        for (String location : web.getResources().getStaticLocations()) {
            Resource index = resources.getResource(location + (location.endsWith("/") ? "" : "/") + "index.html");
            if (index.exists()) {
                try {
                    return index.getContentAsString(StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    // пробуем следующий каталог
                }
            }
        }
        return FALLBACK;
    }

    static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
