package com.curso.instrumento2026.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    private static final String[] CONSULTAS_PUBLICAS = {
            "/api/health", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
            "/swagger-ui.html", "/swagger-ui/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Simplificação didática para clientes de API que enviam Authorization explicitamente.
                // STATELESS não elimina CSRF: navegadores podem enviar Basic automaticamente.
                // Reavaliar esta desativação antes de oferecer acesso autenticado por navegador.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, CONSULTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.HEAD, CONSULTAS_PUBLICAS).permitAll()
                        // Bootstrap provisório; o endpoint de contas será implementado em outra etapa.
                        .requestMatchers(HttpMethod.POST, "/api/pessoas", "/api/contas-acesso").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
