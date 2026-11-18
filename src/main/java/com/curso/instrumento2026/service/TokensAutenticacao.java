package com.curso.instrumento2026.service;

import java.time.Instant;

public record TokensAutenticacao(TokenAcesso accessToken, String refreshToken, Instant refreshTokenExpiresAt) {
    @Override
    public String toString() {
        return "TokensAutenticacao[accessToken=[PROTEGIDO], refreshToken=[PROTEGIDO]]";
    }
}
