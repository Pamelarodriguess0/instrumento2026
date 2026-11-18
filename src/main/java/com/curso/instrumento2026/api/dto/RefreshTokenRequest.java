package com.curso.instrumento2026.api.dto;

// Ausência ou conteúdo em branco é tratado pelo serviço com a resposta genérica de refresh.
public record RefreshTokenRequest(String refreshToken) {
    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=[PROTEGIDO]]";
    }
}
