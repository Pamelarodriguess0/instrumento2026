package com.curso.instrumento2026.exception;

public final class RefreshTokenInvalidoException extends RuntimeException {
    public RefreshTokenInvalidoException() {
        super("Refresh token inválido ou expirado");
    }
}
