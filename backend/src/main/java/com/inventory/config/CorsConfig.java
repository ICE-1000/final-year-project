package com.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowedOriginList = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            // Defensive: strip any trailing slash a config value might carry, since the
            // browser's Origin header never includes one and this list is matched exactly.
            .map(origin -> origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin)
            .filter(origin -> !origin.isEmpty())
            .collect(Collectors.toList());
        if (allowedOriginList.size() == 1 && "*".equals(allowedOriginList.get(0))) {
            configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        } else {
            configuration.setAllowedOrigins(allowedOriginList);
        }
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
