package com.curso.instrumento2026.api.dto;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
        @Schema(description = "JWT de acesso; credencial sensível, não compartilhar ou registrar em logs") String accessToken,
        @Schema(description = "Esquema de autenticação", allowableValues = "Bearer", example = "Bearer") String tokenType,
        @Schema(description = "Validade em segundos", example = "1800") long expiresIn,
        @Schema(description = "Instante absoluto de expiração UTC", format = "date-time") Instant expiresAt) {
    @Override
    public String toString() {
        return "TokenResponse[accessToken=[PROTEGIDO], tokenType=" + tokenType
                + ", expiresIn=" + expiresIn + ", expiresAt=" + expiresAt + "]";
    }
}
