package com.curso.instrumento2026.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtConfigTest {
    private static final String ISSUER = "https://instrumento2026.local";
    // Dados exclusivamente didáticos, sem ler ou registrar a chave do ambiente local.
    private static final String CHAVE_TESTE = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    private ApplicationContextRunner contexto() {
        return new ApplicationContextRunner().withUserConfiguration(JwtConfig.class)
                .withPropertyValues("app.security.jwt.issuer=" + ISSUER,
                        "app.security.jwt.expiration=PT30M");
    }

    private ApplicationContextRunner contextoValido() {
        return contexto().withPropertyValues("app.security.jwt.secret=" + CHAVE_TESTE);
    }

    private JwtClaimsSet claimsValidos(String issuer) {
        Instant emitido = Instant.now().minusSeconds(5);
        return JwtClaimsSet.builder().issuer(issuer).subject("conta.jwt.teste")
                .issuedAt(emitido).expiresAt(emitido.plus(Duration.ofMinutes(30))).build();
    }

    @ParameterizedTest
    @ValueSource(ints = {32, 64})
    void deveVincularPropriedadesEAceitarChaveComPeloMenos32Bytes(int tamanho) {
        String chaveDidatica = Base64.getEncoder().encodeToString(new byte[tamanho]);
        contexto().withPropertyValues("app.security.jwt.secret=" + chaveDidatica).run(context -> {
            assertNull(context.getStartupFailure());
            JwtProperties properties = context.getBean(JwtProperties.class);
            SecretKey chave = context.getBean(SecretKey.class);
            assertEquals(ISSUER, properties.issuer());
            assertEquals(Duration.ofMinutes(30), properties.expiration());
            assertEquals(tamanho, chave.getEncoded().length);
            assertEquals("HmacSHA256", chave.getAlgorithm());
            assertNotNull(context.getBean(JwtEncoder.class));
            assertNotNull(context.getBean(JwtDecoder.class));
            assertFalse(properties.toString().contains(chaveDidatica));
            assertTrue(properties.toString().contains("[PROTEGIDA]"));
        });
    }

    @Test
    void deveRejeitarBase64InvalidaSemExporEntrada() {
        verificarChaveInvalida("nao_e_Base64!", "JWT_SECRET deve estar codificada em Base64");
    }

    @Test
    void deveRejeitarChaveDecodificadaInsuficiente() {
        verificarChaveInvalida(Base64.getEncoder().encodeToString(new byte[31]),
                "JWT_SECRET deve possuir pelo menos 32 bytes");
    }

    @Test
    void deveRejeitarAusenciaDeChave() {
        contexto().run(context -> {
            Throwable falha = causaRaiz(context.getStartupFailure());
            assertInstanceOf(IllegalStateException.class, falha);
            assertEquals("JWT_SECRET é obrigatória", falha.getMessage());
        });
    }

    private void verificarChaveInvalida(String entrada, String mensagem) {
        contexto().withPropertyValues("app.security.jwt.secret=" + entrada).run(context -> {
            Throwable falha = causaRaiz(context.getStartupFailure());
            assertInstanceOf(IllegalStateException.class, falha);
            assertEquals(mensagem, falha.getMessage());
            assertFalse(context.getStartupFailure().toString().contains(entrada));
            assertNull(falha.getCause());
        });
    }

    private Throwable causaRaiz(Throwable falha) {
        assertNotNull(falha);
        while (falha.getCause() != null) falha = falha.getCause();
        return falha;
    }

    @Test
    void deveAssinarEValidarHS256ComEmissorEDuracaoConfigurados() {
        contextoValido().run(context -> {
            var encoder = context.getBean(JwtEncoder.class);
            var decoder = context.getBean(JwtDecoder.class);
            String token = encoder.encode(JwtEncoderParameters.from(claimsValidos(ISSUER))).getTokenValue();
            Jwt validado = decoder.decode(token);
            assertEquals("HS256", validado.getHeaders().get("alg"));
            assertEquals(ISSUER, validado.getIssuer().toString());
            assertEquals("conta.jwt.teste", validado.getSubject());
            assertEquals(context.getBean(JwtProperties.class).expiration(),
                    Duration.between(validado.getIssuedAt(), validado.getExpiresAt()));
        });
    }

    @Test
    void deveRejeitarAssinaturaDeOutraChave() {
        contextoValido().run(context -> {
            var outroEncoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(new byte[32], "HmacSHA256"))
                    .algorithm(MacAlgorithm.HS256).build();
            String token = outroEncoder.encode(JwtEncoderParameters.from(claimsValidos(ISSUER))).getTokenValue();
            assertThrows(JwtException.class, () -> context.getBean(JwtDecoder.class).decode(token));
        });
    }

    @Test
    void deveRejeitarEmissorIncorretoMesmoComAssinaturaValida() {
        contextoValido().run(context -> {
            String token = context.getBean(JwtEncoder.class).encode(JwtEncoderParameters.from(
                    claimsValidos("https://outro-emissor.invalid"))).getTokenValue();
            assertThrows(JwtValidationException.class, () -> context.getBean(JwtDecoder.class).decode(token));
        });
    }

    @Test
    void deveRejeitarTokenExpiradoAlemDaToleranciaTemporalPadrao() {
        contextoValido().run(context -> {
            Instant agora = Instant.now();
            var claims = JwtClaimsSet.builder().issuer(ISSUER).subject("conta.jwt.teste")
                    .issuedAt(agora.minusSeconds(3600)).expiresAt(agora.minusSeconds(180)).build();
            String token = context.getBean(JwtEncoder.class).encode(JwtEncoderParameters.from(claims)).getTokenValue();
            assertThrows(JwtValidationException.class, () -> context.getBean(JwtDecoder.class).decode(token));
        });
    }

    @Test
    void deveRejeitarTokenAindaNaoValido() {
        contextoValido().run(context -> {
            Instant agora = Instant.now();
            var claims = JwtClaimsSet.builder().issuer(ISSUER).subject("conta.jwt.teste")
                    .issuedAt(agora.minusSeconds(5)).notBefore(agora.plusSeconds(180))
                    .expiresAt(agora.plusSeconds(1800)).build();
            String token = context.getBean(JwtEncoder.class).encode(JwtEncoderParameters.from(claims)).getTokenValue();
            assertThrows(JwtValidationException.class, () -> context.getBean(JwtDecoder.class).decode(token));
        });
    }

    @Test
    void deveRejeitarHS512MesmoAssinadoComOsMesmosBytesDaChave() {
        String chaveDidatica = Base64.getEncoder().encodeToString(new byte[64]);
        contexto().withPropertyValues("app.security.jwt.secret=" + chaveDidatica).run(context -> {
            SecretKey chave = context.getBean(SecretKey.class);
            var outroEncoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(chave.getEncoded(), "HmacSHA512"))
                    .algorithm(MacAlgorithm.HS512).build();
            String token = outroEncoder.encode(JwtEncoderParameters.from(claimsValidos(ISSUER))).getTokenValue();
            assertThrows(JwtException.class, () -> context.getBean(JwtDecoder.class).decode(token));
        });
    }
}
