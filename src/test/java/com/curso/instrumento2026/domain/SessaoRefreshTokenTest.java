package com.curso.instrumento2026.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SessaoRefreshTokenTest {
    private static final Instant CRIACAO = Instant.parse("2026-11-16T12:00:00Z");
    private static final Instant EXPIRACAO = CRIACAO.plus(Duration.ofHours(8));
    private static final String FAMILIA = "00000000-0000-0000-0000-000000000001";
    private static final String HASH_SINTETICO = "a".repeat(64);

    private ContaAcesso conta() {
        return new ContaAcesso(new Pessoa("Ana", "ana@example.com", "52998224725",
                LocalDate.of(2026, 11, 16)), "sessao.teste", "hash-sintetico-sem-credencial-real");
    }

    private SessaoRefreshToken sessao() {
        return new SessaoRefreshToken(conta(), FAMILIA, HASH_SINTETICO, CRIACAO, CRIACAO, EXPIRACAO);
    }

    @Test
    void deveCriarGeracaoSemUsoOuRevogacaoPreservandoContaHashELimite() {
        var conta = conta();
        var sessao = new SessaoRefreshToken(conta, " " + FAMILIA + " ",
                " " + HASH_SINTETICO + " ", CRIACAO, CRIACAO, EXPIRACAO);
        assertNull(sessao.getId());
        assertSame(conta, sessao.getConta());
        assertEquals(FAMILIA, sessao.getFamiliaId());
        assertEquals(HASH_SINTETICO, sessao.getTokenHash());
        assertEquals(CRIACAO, sessao.getCriadaEm());
        assertEquals(CRIACAO, sessao.getUltimaAtividadeEm());
        assertEquals(EXPIRACAO, sessao.getExpiraEm());
        assertFalse(sessao.foiUtilizada());
        assertFalse(sessao.foiRevogada());
        assertNull(sessao.getMotivoRevogacao());
    }

    @Test
    void deveExigirContaTextosEInstantes() {
        assertThrows(NullPointerException.class,
                () -> new SessaoRefreshToken(null, FAMILIA, HASH_SINTETICO, CRIACAO, CRIACAO, EXPIRACAO));
        for (String texto : new String[]{null, "", " "}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new SessaoRefreshToken(conta(), texto, HASH_SINTETICO, CRIACAO, CRIACAO, EXPIRACAO));
            assertThrows(IllegalArgumentException.class,
                    () -> new SessaoRefreshToken(conta(), FAMILIA, texto, CRIACAO, CRIACAO, EXPIRACAO));
        }
        assertThrows(NullPointerException.class,
                () -> new SessaoRefreshToken(conta(), FAMILIA, HASH_SINTETICO, null, CRIACAO, EXPIRACAO));
        assertThrows(NullPointerException.class,
                () -> new SessaoRefreshToken(conta(), FAMILIA, HASH_SINTETICO, CRIACAO, null, EXPIRACAO));
        assertThrows(NullPointerException.class,
                () -> new SessaoRefreshToken(conta(), FAMILIA, HASH_SINTETICO, CRIACAO, CRIACAO, null));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void deveExigirExpiracaoPosteriorACriacao(int segundos) {
        assertThrows(IllegalArgumentException.class, () -> new SessaoRefreshToken(
                conta(), FAMILIA, HASH_SINTETICO, CRIACAO, CRIACAO, CRIACAO.plusSeconds(segundos)));
    }

    @Test
    void deveExpirarExatamenteNoLimiteDeInatividade() {
        var sessao = sessao();
        var idle = Duration.ofMinutes(30);
        var limite = CRIACAO.plus(idle);
        assertFalse(sessao.expirouPorInatividade(limite.minusNanos(1), idle));
        assertTrue(sessao.expirouPorInatividade(limite, idle));
        assertTrue(sessao.expirouPorInatividade(limite.plusNanos(1), idle));
    }

    @Test
    void deveContarInatividadeDesdeUltimaAtividadeMesmoQuandoDiferenteDaCriacao() {
        var ultimaAtividade = CRIACAO.plus(Duration.ofMinutes(10));
        var idle = Duration.ofMinutes(30);
        var sessao = new SessaoRefreshToken(conta(), FAMILIA, HASH_SINTETICO,
                CRIACAO, ultimaAtividade, EXPIRACAO);

        assertFalse(sessao.expirouPorInatividade(CRIACAO.plus(idle), idle));
        assertFalse(sessao.expirouPorInatividade(ultimaAtividade.plus(idle).minusNanos(1), idle));
        assertTrue(sessao.expirouPorInatividade(ultimaAtividade.plus(idle), idle));
        assertEquals(ultimaAtividade, sessao.getUltimaAtividadeEm());
        assertEquals(EXPIRACAO, sessao.getExpiraEm());
    }

    @Test
    void deveExpirarExatamenteNoLimiteAbsolutoSemDeslocaLoAoUsar() {
        var sessao = sessao();
        sessao.marcarUtilizada(CRIACAO.plusSeconds(10));
        assertFalse(sessao.expirouAbsolutamente(EXPIRACAO.minusNanos(1)));
        assertTrue(sessao.expirouAbsolutamente(EXPIRACAO));
        assertTrue(sessao.expirouAbsolutamente(EXPIRACAO.plusNanos(1)));
        assertEquals(EXPIRACAO, sessao.getExpiraEm());
    }

    @Test
    void deveMarcarUsoUmaUnicaVezSemApagarHistorico() {
        var sessao = sessao();
        assertThrows(NullPointerException.class, () -> sessao.marcarUtilizada(null));
        assertFalse(sessao.foiUtilizada());
        var uso = CRIACAO.plusSeconds(10);
        sessao.marcarUtilizada(uso);
        assertTrue(sessao.foiUtilizada());
        assertThrows(IllegalStateException.class, () -> sessao.marcarUtilizada(uso.plusSeconds(1)));
        assertEquals(uso, sessao.getUtilizadaEm());
        assertEquals(HASH_SINTETICO, sessao.getTokenHash());
    }

    @Test
    void devePreservarPrimeiroMotivoEInstanteDeRevogacao() {
        var sessao = sessao();
        var uso = CRIACAO.plusSeconds(1);
        sessao.marcarUtilizada(uso);
        var instante = CRIACAO.plusSeconds(10);
        sessao.revogar(instante, MotivoRevogacaoRefreshToken.LOGOUT);
        sessao.revogar(instante.plusSeconds(1), MotivoRevogacaoRefreshToken.REUTILIZACAO);
        assertTrue(sessao.foiRevogada());
        assertEquals(instante, sessao.getRevogadaEm());
        assertEquals(MotivoRevogacaoRefreshToken.LOGOUT, sessao.getMotivoRevogacao());
        assertTrue(sessao.foiUtilizada());
        assertEquals(uso, sessao.getUtilizadaEm());
        assertEquals(EXPIRACAO, sessao.getExpiraEm());
    }

    @Test
    void revogacaoInvalidaNaoDeveDeixarEstadoParcial() {
        var sessao = sessao();
        assertThrows(NullPointerException.class,
                () -> sessao.revogar(null, MotivoRevogacaoRefreshToken.LOGOUT));
        assertThrows(NullPointerException.class, () -> sessao.revogar(CRIACAO, null));
        assertFalse(sessao.foiRevogada());
        assertNull(sessao.getMotivoRevogacao());
    }
}
