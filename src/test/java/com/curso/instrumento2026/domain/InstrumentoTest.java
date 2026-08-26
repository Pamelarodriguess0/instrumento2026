package com.curso.instrumento2026.domain;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InstrumentoTest {
    @Test
    void deveCriarInstrumentoValido() {
        CategoriaInstrumento categoria = new CategoriaInstrumento(1L, "Cordas");
        Instrumento instrumento = new Instrumento(
                1L,
                "INT001",
                "Violoncelo Eagle",
                5,
                new BigDecimal("5000.00"),
                LocalDate.of(2026,6,15),
                Status.ATIVO,
                categoria
        );

        assertEquals("INT001", instrumento.getCodigoInstrumento());
        assertEquals("Violoncelo Eagle", instrumento.getNomeInstrumento());
        assertEquals(5, instrumento.getQuantidadeEstoque());
        assertEquals(Status.ATIVO, instrumento.getStatus());
        assertEquals(categoria, instrumento.getCategoria());
    }
    @Test
    void naoDeveCriarInstrumentoComEstoqueNegativo() {
        CategoriaInstrumento categoria =
                new CategoriaInstrumento(1L, "Cordas");
        assertThrows(IllegalArgumentException.class, () ->
                new Instrumento(
                        1L,
                        "INT001",
                        "Violoncelo Eagle",
                        -5,
                        new BigDecimal("5000.00"),
                        LocalDate.of(2026, 6, 15),
                        Status.ATIVO,
                        categoria
                )
        );
    }

    @Test
    void deveCalcularValorEstoque() {
        CategoriaInstrumento categoria = new CategoriaInstrumento(1L, "Cordas");
        Instrumento instrumento = new Instrumento(
                1L,
                "INT001",
                "Violoncelo Eagle",
                5,
                new BigDecimal("5000.00"),
                LocalDate.of(2026,6,15),
                Status.ATIVO,
                categoria
        );

        BigDecimal valorEstoque = instrumento.calcularValorEstoque();
        assertEquals(new BigDecimal("25000.00"), valorEstoque);
    }

    @Test
    void deveAdicionarEstoque() {
        CategoriaInstrumento categoria =
                new CategoriaInstrumento(1L, "Cordas");
        Instrumento instrumento = new Instrumento(
                1L,
                "INT001",
                "Violoncelo Eagle",
                5,
                new BigDecimal("5000.00"),
                LocalDate.of(2026, 6, 15),
                Status.ATIVO,
                categoria
        );

        instrumento.adicionarEstoque(3);
        assertEquals(8, instrumento.getQuantidadeEstoque());
    }

    @Test
    void deveRetirarEstoque() {
        CategoriaInstrumento categoria =
                new CategoriaInstrumento(1L, "Cordas");
        Instrumento instrumento = new Instrumento(
                1L,
                "INT001",
                "Violoncelo Eagle",
                5,
                new BigDecimal("5000.00"),
                LocalDate.of(2026, 6, 15),
                Status.ATIVO,
                categoria
        );
        instrumento.retirarEstoque(2);
        assertEquals(3, instrumento.getQuantidadeEstoque());
    }

    @Test
    void naoDeveRetirarMaisQueOEstoque() {
        CategoriaInstrumento categoria =
                new CategoriaInstrumento(1L, "Cordas");

        Instrumento instrumento = new Instrumento(
                1L,
                "INT001",
                "Violoncelo Eagle",
                5,
                new BigDecimal("5000.00"),
                LocalDate.of(2026, 6, 15),
                Status.ATIVO,
                categoria
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> instrumento.retirarEstoque(8)
        );
    }

    @Test
    void deveInativarInstrumento() {
        CategoriaInstrumento categoria =
                new CategoriaInstrumento(1L, "Cordas");

        Instrumento instrumento = new Instrumento(
                1L,
                "INT001",
                "Violoncelo Eagle",
                5,
                new BigDecimal("5000.00"),
                LocalDate.of(2026, 6, 15),
                Status.ATIVO,
                categoria
        );
        instrumento.inativar();
        assertEquals(Status.INATIVO, instrumento.getStatus());
    }

    @Test
    void deveAdicionarInstrumentoNaCategoria() {
        CategoriaInstrumento categoria =
                new CategoriaInstrumento(1L, "Cordas");

        Instrumento instrumento = new Instrumento(
                1L,
                "INT001",
                "Violoncelo Eagle",
                5,
                new BigDecimal("5000.00"),
                LocalDate.of(2026, 6, 15),
                Status.ATIVO,
                categoria
        );
        categoria.adicionarInstrumento(instrumento);
        assertEquals(1, categoria.getInstrumentos().size());
        assertEquals(instrumento, categoria.getInstrumentos().get(0));
    }
}
