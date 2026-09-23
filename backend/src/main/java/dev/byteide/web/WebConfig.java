package dev.byteide.web;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimit;
    private final WebProperties web;

    public WebConfig(RateLimitInterceptor rateLimit, WebProperties web) {
        this.rateLimit = rateLimit;
        this.web = web;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimit).addPathPatterns("/api/**");
    }

    /**
     * Файлы сборки фронтенда содержат хеш в имени, поэтому кешируются навсегда.
     * Если рядом лежат предсжатые .br/.gz, отдаются они.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String[] assets = Arrays.stream(web.getResources().getStaticLocations())
                .map(location -> location + (location.endsWith("/") ? "" : "/") + "assets/")
                .toArray(String[]::new);
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(assets)
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .resourceChain(true)
                .addResolver(new EncodedResourceResolver())
                .addResolver(new PathResourceResolver());
    }
}
