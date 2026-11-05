package com.curso.instrumento2026.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Validated explicitly by the service only when creation is enabled and necessary.
@ConfigurationProperties(prefix = "app.security.initial-admin")
public record AdministradorInicialProperties(
        boolean enabled,
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank String cpf,
        @NotBlank String login,
        @NotBlank String senha) {
    @Override
    public String toString() {
        return "AdministradorInicialProperties[enabled=" + enabled + ", dados=[PROTEGIDOS]]";
    }
}
