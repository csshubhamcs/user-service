package com.shikshaspace.userservice.config;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/** CORS configuration for cross-origin requests. TODO: Restrict origins in production! */
@Slf4j
@Configuration
public class CorsConfig {

  @Bean
  public CorsWebFilter corsWebFilter() {
    log.warn("⚠️ CORS allowing ALL origins - RESTRICT THIS IN PRODUCTION!");

    CorsConfiguration corsConfig = new CorsConfiguration();

    // ⚠️ DEVELOPMENT ONLY - Allow all origins
    corsConfig.addAllowedOriginPattern("*");

    // Allow all HTTP methods
    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

    // Allow all headers
    corsConfig.addAllowedHeader("*");

    // Allow credentials (cookies, authorization headers)
    corsConfig.setAllowCredentials(true);

    // Cache preflight response for 1 hour
    corsConfig.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);

    return new CorsWebFilter(source);
  }
}
