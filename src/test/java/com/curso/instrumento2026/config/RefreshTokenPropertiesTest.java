package com.curso.instrumento2026.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.ResourcePropertySource;

import javax.sql.DataSource;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenPropertiesTest {
    private ApplicationContextRunner contexto() {
        return new ApplicationContextRunner().withUserConfiguration(RefreshTokenConfig.class)
                .withInitializer(ctx -> {
                    try {
                        ctx.getEnvironment().getPropertySources().addLast(
                                new ResourcePropertySource("classpath:application.properties"));
                    } catch (java.io.IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                });
    }

    @Test
    void deveRegistrarDefaultsSemBancoOuConfiguracaoJwt() {
        contexto().run(ctx -> {
            assertNull(ctx.getStartupFailure());
            var p = ctx.getBean(RefreshTokenProperties.class);
            assertEquals(Duration.ofMinutes(30), p.idleTimeout());
            assertEquals(Duration.ofHours(8), p.absoluteExpiration());
            assertTrue(ctx.getBeansOfType(DataSource.class).isEmpty());
            assertTrue(ctx.getBeansOfType(JwtProperties.class).isEmpty());
        });
    }

    @Test
    void deveVincularOverridesDasVariaveisDeAmbiente() {
        contexto().withPropertyValues("REFRESH_TOKEN_IDLE_TIMEOUT=PT10M",
                "REFRESH_TOKEN_ABSOLUTE_EXPIRATION=PT2H").run(ctx -> {
            assertNull(ctx.getStartupFailure());
            var p = ctx.getBean(RefreshTokenProperties.class);
            assertEquals(Duration.ofMinutes(10), p.idleTimeout());
            assertEquals(Duration.ofHours(2), p.absoluteExpiration());
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"PT0S", "PT-1S"})
    void deveRejeitarInatividadeAusenteOuNaoPositiva(String valor) {
        var idle = valor == null ? null : Duration.parse(valor);
        assertThrows(IllegalArgumentException.class, () -> new RefreshTokenProperties(idle, Duration.ofHours(8)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"PT0S", "PT-1S"})
    void deveRejeitarValidadeAbsolutaAusenteOuNaoPositiva(String valor) {
        var absoluta = valor == null ? null : Duration.parse(valor);
        assertThrows(IllegalArgumentException.class,
                () -> new RefreshTokenProperties(Duration.ofMinutes(30), absoluta));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT10M", "PT30M"})
    void deveExigirValidadeAbsolutaMaiorQueInatividade(String valor) {
        assertThrows(IllegalArgumentException.class,
                () -> new RefreshTokenProperties(Duration.ofMinutes(30), Duration.parse(valor)));
    }

    @Test
    void bindingDeveFalharParaDuracaoMalformada() {
        contexto().withPropertyValues("app.security.refresh-token.idle-timeout=trinta")
                .run(ctx -> assertNotNull(ctx.getStartupFailure()));
    }
}
