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

class ProtecaoAutenticacaoPropertiesTest {
    private ApplicationContextRunner contexto() {
        return new ApplicationContextRunner().withUserConfiguration(ProtecaoAutenticacaoConfig.class)
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
    void deveRegistrarPadroesSemBancoJwtOuSessao() {
        contexto().run(ctx -> {
            assertNull(ctx.getStartupFailure());
            var p = ctx.getBean(ProtecaoAutenticacaoProperties.class);
            assertEquals(5, p.maxFailedAttempts());
            assertEquals(Duration.ofMinutes(15), p.failureWindow());
            assertEquals(Duration.ofMinutes(15), p.lockDuration());
            assertTrue(ctx.getBeansOfType(DataSource.class).isEmpty());
            assertTrue(ctx.getBeansOfType(JwtProperties.class).isEmpty());
            assertTrue(ctx.getBeansOfType(RefreshTokenProperties.class).isEmpty());
        });
    }

    @Test
    void deveVincularVariaveisDaPolitica() {
        contexto().withPropertyValues("AUTH_MAX_FAILED_ATTEMPTS=3", "AUTH_FAILURE_WINDOW=PT10M",
                "AUTH_LOCK_DURATION=PT20M").run(ctx -> {
            assertNull(ctx.getStartupFailure());
            var p = ctx.getBean(ProtecaoAutenticacaoProperties.class);
            assertEquals(3, p.maxFailedAttempts());
            assertEquals(Duration.ofMinutes(10), p.failureWindow());
            assertEquals(Duration.ofMinutes(20), p.lockDuration());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void deveRejeitarMaximoNaoPositivo(int maximo) {
        assertThrows(IllegalArgumentException.class,
                () -> new ProtecaoAutenticacaoProperties(maximo, Duration.ofMinutes(15), Duration.ofMinutes(15)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"PT0S", "PT-1S"})
    void deveRejeitarJanelaAusenteOuNaoPositiva(String valor) {
        Duration janela = valor == null ? null : Duration.parse(valor);
        assertThrows(IllegalArgumentException.class,
                () -> new ProtecaoAutenticacaoProperties(5, janela, Duration.ofMinutes(15)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"PT0S", "PT-1S"})
    void deveRejeitarBloqueioAusenteOuNaoPositivo(String valor) {
        Duration bloqueio = valor == null ? null : Duration.parse(valor);
        assertThrows(IllegalArgumentException.class,
                () -> new ProtecaoAutenticacaoProperties(5, Duration.ofMinutes(15), bloqueio));
    }

    @ParameterizedTest
    @ValueSource(strings = {"max-failed-attempts=0", "max-failed-attempts=-1", "failure-window=PT0S",
            "failure-window=PT-1S", "lock-duration=PT0S", "lock-duration=PT-1S",
            "failure-window=invalida", "lock-duration=invalida"})
    void configuracaoInvalidaDeveImpedirInicializacao(String propriedade) {
        contexto().withPropertyValues("app.security.authentication-protection." + propriedade)
                .run(ctx -> assertNotNull(ctx.getStartupFailure()));
    }
}
