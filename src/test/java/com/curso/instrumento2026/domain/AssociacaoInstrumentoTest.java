package com.curso.instrumento2026.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.*;

class AssociacaoInstrumentoTest {
    private Instrumento novo(String codigo) {
        return new Instrumento(codigo, "Violino", 1, BigDecimal.TEN, BigDecimal.ZERO, LocalDate.now());
    }

    @Test
    void definirCategoriaIsoladamenteDeveManterOsDoisLados() {
        var origem = new CategoriaInstrumento(null, "Origem");
        var destino = new CategoriaInstrumento(null, "Destino");
        var instrumento = novo("A");
        instrumento.definirCategoria(origem);
        assertSame(origem, instrumento.getCategoria());
        assertTrue(origem.getInstrumentos().contains(instrumento));
        instrumento.definirCategoria(destino);
        assertFalse(origem.getInstrumentos().contains(instrumento));
        assertTrue(destino.getInstrumentos().contains(instrumento));
        assertSame(destino, instrumento.getCategoria());
        destino.adicionarInstrumento(instrumento);
        assertEquals(1, destino.getInstrumentos().size());
    }

    @Test
    void definirCategoriaDevePreservarOrigemQuandoDestinoRejeitaCodigo() {
        var origem = new CategoriaInstrumento(null, "Origem");
        var destino = new CategoriaInstrumento(null, "Destino");
        var instrumento = novo("A");
        var existente = novo("A");
        instrumento.definirCategoria(origem);
        existente.definirCategoria(destino);
        assertThrows(IllegalArgumentException.class, () -> instrumento.definirCategoria(destino));
        assertSame(origem, instrumento.getCategoria());
        assertEquals(java.util.List.of(instrumento), origem.getInstrumentos());
        assertEquals(java.util.List.of(existente), destino.getInstrumentos());
    }

    @Test
    void adicionarEmCategoriaDiferenteDeveRejeitarSemAlterarAssociacao() {
        var origem = new CategoriaInstrumento(null, "Origem");
        var destino = new CategoriaInstrumento(null, "Destino");
        var instrumento = novo("A");
        instrumento.definirCategoria(origem);
        assertThrows(IllegalArgumentException.class, () -> destino.adicionarInstrumento(instrumento));
        assertSame(origem, instrumento.getCategoria());
        assertTrue(origem.getInstrumentos().contains(instrumento));
        assertTrue(destino.getInstrumentos().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> instrumento.definirCategoria(null));
        assertSame(origem, instrumento.getCategoria());
    }

    @Test
    void construtorComCategoriaDeveInicializarOsDoisLados() {
        var categoria = new CategoriaInstrumento(null, "Cordas");
        var instrumento = new Instrumento(null, "A", "Violino", 1, BigDecimal.TEN,
                LocalDate.now(), Status.ATIVO, categoria);
        assertTrue(categoria.getInstrumentos().contains(instrumento));
        categoria.adicionarInstrumento(instrumento);
        assertEquals(1, categoria.getInstrumentos().size());
    }

    @Test
    void remocaoUnilateralNaoDeveSerExpostaForaDoDominio() throws Exception {
        assertFalse(Modifier.isPublic(CategoriaInstrumento.class
                .getDeclaredMethod("removerInstrumento", Instrumento.class).getModifiers()));
    }
}
