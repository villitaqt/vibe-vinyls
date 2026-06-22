package com.vivevinyls.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS para los endpoints de catálogo, consumidos por el frontend React/Vite.
 *
 * <p>El/los origen(es) permitido(s) se parametrizan vía la propiedad
 * {@code app.cors.allowed-origins} (variable de entorno
 * {@code CORS_ALLOWED_ORIGINS}); por defecto el dev server de Vite
 * {@code http://localhost:5173}. Se permite una lista separada por comas, pero
 * nunca el comodín {@code "*"}.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins.split("\\s*,\\s*");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // El catálogo es de solo lectura: basta con GET (y el preflight OPTIONS).
        registry.addMapping("/vinilos")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET");
        registry.addMapping("/vinilos/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET");
    }
}
