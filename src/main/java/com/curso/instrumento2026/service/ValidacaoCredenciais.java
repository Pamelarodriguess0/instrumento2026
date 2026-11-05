package com.curso.instrumento2026.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class ValidacaoCredenciais {
    private ValidacaoCredenciais() { }

    static String normalizarLogin(String login) {
        if (login == null || login.isBlank()) throw new IllegalArgumentException("Login é obrigatório");
        String normalizado = login.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() < 3 || normalizado.length() > 80) {
            throw new IllegalArgumentException("Login deve possuir entre 3 e 80 caracteres");
        }
        if (!normalizado.matches("^[a-z0-9._-]+$")) {
            throw new IllegalArgumentException("Login aceita somente letras, números, ponto, hífen e sublinhado");
        }
        return normalizado;
    }

    static void validarSenha(String senha) {
        if (senha == null || senha.isBlank()) throw new IllegalArgumentException("Senha é obrigatória");
        if (senha.codePointCount(0, senha.length()) < 12) {
            throw new IllegalArgumentException("Senha deve possuir ao menos 12 caracteres");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Senha deve possuir no máximo 72 bytes em UTF-8");
        }
    }
}
