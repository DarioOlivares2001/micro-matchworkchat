package com.matchwork.chatservice.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class GlobalCorsConfig {

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    // Permite tu front (o usa "*")
    config.addAllowedOriginPattern("http://localhost:4200");
    // O si quieres abrirlo a todos:
    // config.addAllowedOriginPattern("*");
    
    // Header personalizado (ej. Authorization)
    config.setAllowedHeaders(List.of("*"));
    // Métodos que aceptas
    config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    // Si lo necesitas, expón ciertos headers en la respuesta
    config.setExposedHeaders(List.of("Authorization"));
    // Si usas cookies o auth con credenciales
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    return new CorsFilter(source);
  }
}
