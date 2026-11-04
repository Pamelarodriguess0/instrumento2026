package com.curso.instrumento2026.service;

import com.curso.instrumento2026.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.spec.SecretKeySpec;
import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {
    @Test
    void deveUsarClockInjetadoEPerfisAutenticadosSemIncluirCredenciais() {
        Instant instante = Instant.parse("2026-11-03T12:00:00.123456Z");
        var key = new SecretKeySpec(new byte[32], "HmacSHA256");
        var encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
        var props = new JwtProperties("https://instrumento2026.local", Duration.ofMinutes(30), "nao-utilizada-neste-teste");
        var service = new TokenService(encoder, props, Clock.fixed(instante, ZoneOffset.UTC));
        var auth = UsernamePasswordAuthenticationToken.authenticated("operador.teste", "credencial-ficticia", List.of(
                new SimpleGrantedAuthority("ROLE_OPERADOR"), new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"),
                new SimpleGrantedAuthority("SCOPE_outro")));
        var token = service.emitir(auth);
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var tempo = new JwtTimestampValidator();
        tempo.setClock(Clock.fixed(instante, ZoneOffset.UTC));
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                tempo, new JwtIssuerValidator(props.issuer())));
        var jwt = decoder.decode(token.valor());
        assertEquals(Instant.parse("2026-11-03T12:00:00Z"), jwt.getIssuedAt());
        assertEquals(Instant.parse("2026-11-03T12:30:00Z"), token.expiraEm());
        assertEquals(1800, token.duracaoSegundos());
        assertEquals(List.of("ADMINISTRADOR", "OPERADOR"), jwt.getClaimAsStringList("perfis"));
        assertFalse(jwt.getClaims().toString().contains("credencial-ficticia"));
        assertFalse(token.toString().contains(token.valor()));
    }
}
