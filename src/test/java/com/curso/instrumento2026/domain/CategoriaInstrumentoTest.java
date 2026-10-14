package com.curso.instrumento2026.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
public class CategoriaInstrumentoTest {

    @Test
    void deveAlterarNomeDaCategoria() {
        CategoriaInstrumento categoria =
                new CategoriaInstrumento(1L, "Cordas");
        categoria.alterarNome("Sopro");
        assertEquals("Sopro", categoria.getNome());
    }

    @Test
    void naoDeveAlterarNomeParaVazio() {
        CategoriaInstrumento categoria =
                new CategoriaInstrumento(1L, "Cordas");
        assertThrows(
                IllegalArgumentException.class,
                () -> categoria.alterarNome("")
        );
    }
}
