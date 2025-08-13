package io.artur.interview.kanga.spread_ranking.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * CORS configuration for the spread ranking API.
 * Allows cross-origin requests from configured origins.
 */
@Configuration
@Slf4j
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private List<String> allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private List<String> allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private List<String> allowedHeaders;

    @Value("${app.cors.exposed-headers:}")
    private List<String> exposedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age-seconds:3600}")
    private long maxAgeSeconds;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins
        configuration.setAllowedOriginPatterns(allowedOrigins);
        log.info("CORS allowed origins: {}", allowedOrigins);
        
        // Set allowed methods
        configuration.setAllowedMethods(allowedMethods);
        log.debug("CORS allowed methods: {}", allowedMethods);
        
        // Set allowed headers
        if (allowedHeaders.contains("*")) {
            configuration.addAllowedHeader("*");
        } else {
            configuration.setAllowedHeaders(allowedHeaders);
        }
        log.debug("CORS allowed headers: {}", allowedHeaders);
        
        // Set exposed headers if any
        if (!exposedHeaders.isEmpty()) {
            configuration.setExposedHeaders(exposedHeaders);
            log.debug("CORS exposed headers: {}", exposedHeaders);
        }
        
        // Set credentials support
        configuration.setAllowCredentials(allowCredentials);
        log.debug("CORS allow credentials: {}", allowCredentials);
        
        // Set preflight cache duration
        configuration.setMaxAge(Duration.ofSeconds(maxAgeSeconds));
        log.debug("CORS max age: {} seconds", maxAgeSeconds);
        
        // Apply configuration to all API endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        log.info("CORS configuration initialized for /api/** endpoints");
        
        return source;
    }
}