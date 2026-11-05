package com.curso.instrumento2026.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.function.Supplier;

/** Scoped authority for test fixture setup; never leaves an administrator on subsequent HTTP requests. */
public final class AdminFixtureSupport {
    private AdminFixtureSupport() { }

    public static <T> T comoAdministrador(Supplier<T> operacao) {
        var anterior = SecurityContextHolder.getContext();
        var administrativo = SecurityContextHolder.createEmptyContext();
        administrativo.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "administrador.fixture", null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));
        try {
            SecurityContextHolder.setContext(administrativo);
            return operacao.get();
        } finally {
            SecurityContextHolder.setContext(anterior);
        }
    }
}
