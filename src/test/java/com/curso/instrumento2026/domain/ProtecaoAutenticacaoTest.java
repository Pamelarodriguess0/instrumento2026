package com.curso.instrumento2026.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ProtecaoAutenticacaoTest {
    private static final Instant INICIO = Instant.parse("2026-11-18T12:00:00Z");
    private static final Duration PRAZO = Duration.ofMinutes(15);

    private ProtecaoAutenticacao novaProtecao() {
        return new ProtecaoAutenticacao("a".repeat(64));
    }

    @ParameterizedTest
    @ValueSource(strings = {"192.0.2.10\r", "192.0.2.10\n"})
    void eventoDeveRejeitarQuebrasDeLinhaNaOrigem(String origem) {
        assertThrows(IllegalArgumentException.class, () -> new EventoSeguranca(
                TipoEventoSeguranca.LOGIN_FALHA, INICIO, null, null, "a".repeat(64), origem));
    }

    @Test
    void eventoDeveRejeitarIdentificadorForaDoFormatoSha256() {
        assertThrows(IllegalArgumentException.class, () -> new EventoSeguranca(
                TipoEventoSeguranca.LOGIN_FALHA, INICIO, null, null, "g".repeat(64), "192.0.2.10"));
    }

    @Test
    void deveAcumularPrimeirasFalhasNaMesmaJanelaSemBloquear() {
        var protecao = novaProtecao();
        assertNull(protecao.getId());
        assertEquals("a".repeat(64), protecao.getChaveLogin());
        assertEquals(0, protecao.getFalhasConsecutivas());
        assertFalse(protecao.estaBloqueada(INICIO));
        for (int tentativa = 1; tentativa <= 4; tentativa++) {
            assertFalse(protecao.registrarFalha(INICIO.plusSeconds((tentativa - 1) * 60L), 5, PRAZO, PRAZO));
            assertEquals(tentativa, protecao.getFalhasConsecutivas());
            assertEquals(INICIO, protecao.getJanelaIniciadaEm());
            assertNull(protecao.getBloqueadaAte());
        }
    }

    @Test
    void deveAtivarBloqueioNaQuintaFalhaAPartirDoInstanteDaFalha() {
        var protecao = novaProtecao();
        for (int i = 0; i < 4; i++) protecao.registrarFalha(INICIO.plusSeconds(i * 60L), 5, PRAZO, PRAZO);
        Instant quinta = INICIO.plusSeconds(240);
        assertTrue(protecao.registrarFalha(quinta, 5, PRAZO, PRAZO));
        assertEquals(5, protecao.getFalhasConsecutivas());
        assertEquals(quinta.plus(PRAZO), protecao.getBloqueadaAte());
        assertTrue(protecao.estaBloqueada(quinta));
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void deveRespeitarFronteiraDaJanela(long nanos) {
        var protecao = novaProtecao();
        protecao.registrarFalha(INICIO, 5, PRAZO, PRAZO);
        Instant novaFalha = INICIO.plus(PRAZO).plusNanos(nanos);
        assertFalse(protecao.registrarFalha(novaFalha, 5, PRAZO, PRAZO));
        assertEquals(nanos < 0 ? 2 : 1, protecao.getFalhasConsecutivas());
        assertEquals(nanos < 0 ? INICIO : novaFalha, protecao.getJanelaIniciadaEm());
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void deveLiberarExatamenteNoVencimentoDoBloqueio(long nanos) {
        var protecao = novaProtecao();
        for (int i = 0; i < 5; i++) protecao.registrarFalha(INICIO, 5, PRAZO, PRAZO);
        assertEquals(nanos < 0, protecao.estaBloqueada(INICIO.plus(PRAZO).plusNanos(nanos)));
    }

    @Test
    void sucessoDeveLimparTodoEstadoEPermitirNovaJanela() {
        var protecao = novaProtecao();
        for (int i = 0; i < 5; i++) protecao.registrarFalha(INICIO, 5, PRAZO, PRAZO);
        protecao.registrarSucesso();
        assertEquals(0, protecao.getFalhasConsecutivas());
        assertNull(protecao.getJanelaIniciadaEm());
        assertNull(protecao.getBloqueadaAte());
        assertFalse(protecao.estaBloqueada(INICIO.plus(PRAZO)));
        assertFalse(protecao.registrarFalha(INICIO.plus(PRAZO), 5, PRAZO, PRAZO));
        assertEquals(1, protecao.getFalhasConsecutivas());
        assertEquals(INICIO.plus(PRAZO), protecao.getJanelaIniciadaEm());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"login.original", "abc", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "gggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg"})
    void deveRejeitarChaveQueNaoSejaHashHexadecimal(String chave) {
        assertThrows(IllegalArgumentException.class, () -> new ProtecaoAutenticacao(chave));
    }
}
