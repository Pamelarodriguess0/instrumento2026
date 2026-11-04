package com.curso.instrumento2026.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String issuer, Duration expiration, String secret) {
    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", expiration=" + expiration + ", secret=[PROTEGIDA]]";
    }
}
