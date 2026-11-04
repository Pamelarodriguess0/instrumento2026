package com.curso.instrumento2026.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Locale;

public record LoginRequest(
        @Schema(description = "Login normalizado com trim e minúsculas")
        @NotBlank @Size(max = 80) String login,
        @Schema(description = "Senha sem normalização; autenticação rejeita mais de 72 bytes UTF-8",
                format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String senha) {
    public LoginRequest {
        login = login == null ? null : login.trim().toLowerCase(Locale.ROOT);
    }

    // O PasswordEncoder rejeita acima de 72 bytes UTF-8; não limitar apenas caracteres no DTO.
    @Override
    public String toString() {
        return "LoginRequest[login=" + login + ", senha=[PROTEGIDA]]";
    }
}
