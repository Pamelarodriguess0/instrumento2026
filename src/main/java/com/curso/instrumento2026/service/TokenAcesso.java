package com.curso.instrumento2026.service;

import java.time.Instant;

public record TokenAcesso(String valor, Instant expiraEm, long duracaoSegundos) {
    @Override
    public String toString() {
        return "TokenAcesso[valor=[PROTEGIDO], expiraEm=" + expiraEm + ", duracaoSegundos=" + duracaoSegundos + "]";
    }
}
