package com.curso.instrumento2026.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class CorrecaoAula08Test {
    private Instrumento instrumento(String codigo, CategoriaInstrumento categoria) {
        var instrumento = new Instrumento(null, codigo, "Violino", 5,
                new BigDecimal("100.00"), LocalDate.now(), Status.ATIVO, categoria);
        categoria.adicionarInstrumento(instrumento);
        return instrumento;
    }

    @Test
    void deveRejeitarOutroObjetoComMesmoCodigoEAceitarMesmoObjeto() {
        var categoria = new CategoriaInstrumento(null, "Cordas");
        var primeiro = instrumento("COD1", categoria);
        categoria.adicionarInstrumento(primeiro);
        assertThrows(IllegalArgumentException.class, () -> instrumento("COD1", categoria));
        assertEquals(1, categoria.getInstrumentos().size());
        assertSame(primeiro, categoria.getInstrumentos().getFirst());
    }

    @Test
    void deveTrocarCategoriaNosDoisLados() {
        var origem = new CategoriaInstrumento(null, "Origem");
        var destino = new CategoriaInstrumento(null, "Destino");
        var instrumento = instrumento("COD1", origem);
        instrumento.alterarCategoria(destino);
        assertSame(destino, instrumento.getCategoria());
        assertFalse(origem.getInstrumentos().contains(instrumento));
        assertTrue(destino.getInstrumentos().contains(instrumento));
    }

    @Test
    void devePreservarAssociacoesQuandoDestinoPossuiMesmoCodigo() {
        var origem = new CategoriaInstrumento(null, "Origem");
        var destino = new CategoriaInstrumento(null, "Destino");
        var instrumento = instrumento("COD1", origem);
        var existente = instrumento("COD1", destino);
        assertThrows(IllegalArgumentException.class, () -> instrumento.alterarCategoria(destino));
        assertSame(origem, instrumento.getCategoria());
        assertEquals(java.util.List.of(instrumento), origem.getInstrumentos());
        assertEquals(java.util.List.of(existente), destino.getInstrumentos());
    }

    @Test
    void deveManterOrdemQuandoCategoriaNaoMudarERejeitarNula() {
        var categoria = new CategoriaInstrumento(null, "Cordas");
        var primeiro = instrumento("COD1", categoria);
        var segundo = instrumento("COD2", categoria);
        primeiro.alterarCategoria(categoria);
        assertEquals(java.util.List.of(primeiro, segundo), categoria.getInstrumentos());
        assertThrows(IllegalArgumentException.class, () -> primeiro.alterarCategoria(null));
        assertSame(categoria, primeiro.getCategoria());
        assertEquals(java.util.List.of(primeiro, segundo), categoria.getInstrumentos());
    }

    @Test
    void deveAceitarLimiteDoEstoqueERejeitarOverflowSemAlterarSaldo() {
        var instrumento = instrumento("COD1", new CategoriaInstrumento(null, "Cordas"));
        assertThrows(IllegalArgumentException.class, () -> instrumento.adicionarEstoque(Integer.MAX_VALUE));
        assertEquals(5, instrumento.getQuantidadeEstoque());
        instrumento.adicionarEstoque(Integer.MAX_VALUE - 5);
        assertEquals(Integer.MAX_VALUE, instrumento.getQuantidadeEstoque());
        assertThrows(IllegalArgumentException.class, () -> instrumento.adicionarEstoque(1));
        assertEquals(Integer.MAX_VALUE, instrumento.getQuantidadeEstoque());
    }

    @Test
    void deveRejeitarMovimentosNaoPositivosSemAlterarSaldo() {
        var instrumento = instrumento("COD1", new CategoriaInstrumento(null, "Cordas"));
        for (int quantidade : new int[]{0, -1, Integer.MIN_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> instrumento.adicionarEstoque(quantidade));
            assertThrows(IllegalArgumentException.class, () -> instrumento.retirarEstoque(quantidade));
        }
        assertEquals(5, instrumento.getQuantidadeEstoque());
    }

    @Test
    void devePermitirRemoverFornecedor() {
        var instrumento = instrumento("COD1", new CategoriaInstrumento(null, "Cordas"));
        var fornecedor = new Fornecedor("Fornecedor", "12345678000195");
        instrumento.alterarFornecedor(fornecedor);
        assertSame(fornecedor, instrumento.getFornecedor());
        instrumento.alterarFornecedor(null);
        assertNull(instrumento.getFornecedor());
    }

    @Test
    void deveNormalizarNomeNaCriacaoEAlteracao() {
        var categoria = new CategoriaInstrumento(null, "  Cordas  ");
        assertEquals("Cordas", categoria.getNome());
        categoria.alterarNome("  Sopro  ");
        assertEquals("Sopro", categoria.getNome());
        assertThrows(IllegalArgumentException.class, () -> categoria.alterarNome("  "));
        assertEquals("Sopro", categoria.getNome());
    }
}
