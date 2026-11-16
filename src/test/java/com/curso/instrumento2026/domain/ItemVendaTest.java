package com.curso.instrumento2026.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ItemVendaTest {
    private static final LocalDate DATA = LocalDate.of(2026, 11, 16);
    private static final BigDecimal PRECO = new BigDecimal("123.45");

    private Venda venda() {
        var pessoa = new Pessoa("Ana", "ana@example.com", "52998224725", DATA);
        return new Venda(new Cliente(pessoa, null),
                new Colaborador(pessoa, "RISCO-ITEM-1", FuncaoColaborador.VENDEDOR, DATA),
                LocalDateTime.of(2026, 11, 16, 10, 0));
    }

    private Instrumento instrumento() {
        return new Instrumento("RISCO-ITEM-1", "Violino", 5, PRECO, BigDecimal.ZERO, DATA);
    }

    @Test
    void deveCriarItemVinculadoAVendaEAoInstrumentoInformados() {
        var venda = venda();
        var instrumento = instrumento();

        var item = new ItemVenda(venda, instrumento, BigDecimal.ONE, PRECO);

        assertSame(venda, item.getVenda());
        assertSame(instrumento, item.getInstrumento());
    }

    @Test
    void deveRejeitarItemSemVendaNaConstrucao() {
        var instrumento = instrumento();

        var erro = assertThrows(NullPointerException.class,
                () -> new ItemVenda(null, instrumento, BigDecimal.ONE, PRECO));

        assertEquals("Venda é obrigatória", erro.getMessage());
    }

    @Test
    void deveRejeitarItemSemInstrumentoNaConstrucao() {
        var venda = venda();

        var erro = assertThrows(NullPointerException.class,
                () -> new ItemVenda(venda, null, BigDecimal.ONE, PRECO));

        assertEquals("Instrumento é obrigatório", erro.getMessage());
    }
}
