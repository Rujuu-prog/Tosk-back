package com.tosk.app.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

  private final List<String> allowedOriginPatterns;

  public CorsConfig(@Value("${app.cors.allowed-origins:}") final String allowedOrigins) {
    if (allowedOrigins == null || allowedOrigins.isBlank()) {
      this.allowedOriginPatterns = List.of("http://localhost:*", "http://127.0.0.1:*");
    } else {
      this.allowedOriginPatterns =
          Arrays.stream(allowedOrigins.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .collect(Collectors.toList());
    }
  }

  @Bean
  @Profile({"local", "docker"})
  public CorsFilter corsFilterForDev() {
    final CorsConfiguration cfg = new CorsConfiguration();
    for (final String pattern : allowedOriginPatterns) {
      cfg.addAllowedOriginPattern(pattern);
    }
    cfg.addAllowedHeader("*");
    cfg.addAllowedMethod("*");
    cfg.setAllowCredentials(true);

    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return new CorsFilter(source);
  }
}
