package com.curso.instrumento2026.service;

import com.curso.instrumento2026.config.JwtProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public TokenAcesso emitir(Authentication authentication) {
        Instant emitidoEm = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiraEm = emitidoEm.plus(properties.expiration());
        var perfis = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(valor -> valor.startsWith("ROLE_"))
                .map(valor -> valor.substring("ROLE_".length())).sorted().toList();
        var claims = JwtClaimsSet.builder().issuer(properties.issuer()).subject(authentication.getName())
                .issuedAt(emitidoEm).expiresAt(expiraEm).id(UUID.randomUUID().toString())
                .claim("perfis", perfis).build();
        String valor = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new TokenAcesso(valor, expiraEm, properties.expiration().getSeconds());
    }
}
