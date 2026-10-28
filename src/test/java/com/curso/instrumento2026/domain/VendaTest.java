package com.curso.instrumento2026.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class VendaTest {
    private Venda venda() {
        var pessoa = new Pessoa("Ana", "ana@example.com", "52998224725", LocalDate.now());
        return new Venda(new Cliente(pessoa, null),
                new Colaborador(pessoa, "VEN-1", FuncaoColaborador.VENDEDOR, LocalDate.now()),
                LocalDateTime.of(2026, 10, 22, 10, 0));
    }

    private Instrumento instrumento(String codigo) {
        return new Instrumento(codigo, "Violino", 5, new BigDecimal("123.45"), BigDecimal.ZERO, LocalDate.now());
    }

    @Test
    void deveAbrirVendaVaziaComTotalZero() {
        var venda = venda();
        assertEquals(StatusVenda.ABERTA, venda.getStatus());
        assertEquals(new BigDecimal("0.00"), venda.calcularTotal());
        assertTrue(venda.getItens().isEmpty());
        assertNotNull(venda.getCliente());
        assertNotNull(venda.getVendedor());
        assertEquals(LocalDateTime.of(2026, 10, 22, 10, 0), venda.getDataVenda());
    }

    @Test
    void deveExigirParticipantesEData() {
        var base = venda();
        assertThrows(NullPointerException.class, () -> new Venda(null, base.getVendedor(), base.getDataVenda()));
        assertThrows(NullPointerException.class, () -> new Venda(base.getCliente(), null, base.getDataVenda()));
        assertThrows(NullPointerException.class, () -> new Venda(base.getCliente(), base.getVendedor(), null));
    }

    @Test
    void deveManterPrecoHistoricoSubtotalETotalSemBaixarEstoque() {
        var venda = venda();
        var instrumento = instrumento("A");
        var item = venda.adicionarItem(instrumento, new BigDecimal("2.000"));
        venda.adicionarItem(instrumento("B"), BigDecimal.ONE);
        instrumento.alterarPrecoUnitario(new BigDecimal("999.00"));
        assertSame(venda, item.getVenda());
        assertSame(instrumento, item.getInstrumento());
        assertEquals(new BigDecimal("123.45"), item.getValorUnitario());
        assertEquals(new BigDecimal("246.90"), item.calcularSubtotal());
        assertEquals(new BigDecimal("370.35"), venda.calcularTotal());
        venda.finalizar();
        assertEquals(5, instrumento.getQuantidadeEstoque());
    }

    @Test
    void deveRejeitarMesmoInstrumentoESameIdSemConfundirObjetosNovos() {
        var venda = venda();
        var primeiro = instrumento("A");
        venda.adicionarItem(primeiro, BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> venda.adicionarItem(primeiro, BigDecimal.ONE));
        venda.adicionarItem(instrumento("B"), BigDecimal.ONE);
        ReflectionTestUtils.setField(primeiro, "id", 1L);
        var mesmaIdentidade = instrumento("A");
        ReflectionTestUtils.setField(mesmaIdentidade, "id", 1L);
        assertThrows(IllegalArgumentException.class, () -> venda.adicionarItem(mesmaIdentidade, BigDecimal.ONE));
        assertEquals(2, venda.getItens().size());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-1", "1.5", "2147483648"})
    void deveRejeitarQuantidadeInvalidaSemAdicionarItem(String quantidade) {
        var venda = venda();
        assertThrows(IllegalArgumentException.class, () -> venda.adicionarItem(instrumento("A"),
                quantidade == null ? null : new BigDecimal(quantidade)));
        assertTrue(venda.getItens().isEmpty());
    }

    @Test
    void deveRejeitarInstrumentoNulo() {
        assertThrows(NullPointerException.class, () -> venda().adicionarItem(null, BigDecimal.ONE));
    }

    @Test
    void deveRemoverItemPeloIdSemAlterarOsDemais() {
        var venda = venda();
        var primeiro = venda.adicionarItem(instrumento("A"), BigDecimal.ONE);
        var segundo = venda.adicionarItem(instrumento("B"), BigDecimal.ONE);
        ReflectionTestUtils.setField(primeiro, "id", 10L);
        ReflectionTestUtils.setField(segundo, "id", 11L);
        venda.removerItem(10L);
        assertEquals(java.util.List.of(segundo), venda.getItens());
        assertEquals(new BigDecimal("123.45"), venda.calcularTotal());
        assertThrows(IllegalArgumentException.class, () -> venda.removerItem(99L));
        assertThrows(IllegalArgumentException.class, () -> venda.removerItem(null));
        assertEquals(1, venda.getItens().size());
    }

    @Test
    void deveProtegerColecaoContraAlteracaoExterna() {
        var venda = venda();
        venda.adicionarItem(instrumento("A"), BigDecimal.ONE);
        assertThrows(UnsupportedOperationException.class, () -> venda.getItens().clear());
    }

    @Test
    void deveRejeitarFinalizacaoVazia() {
        var venda = venda();
        assertThrows(IllegalArgumentException.class, venda::finalizar);
        assertEquals(StatusVenda.ABERTA, venda.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = StatusVenda.class, names = {"FINALIZADA", "CANCELADA"})
    void deveBloquearTodasAsAlteracoesAposEncerramento(StatusVenda estado) {
        var venda = venda();
        var item = venda.adicionarItem(instrumento("A"), BigDecimal.ONE);
        ReflectionTestUtils.setField(item, "id", 10L);
        if (estado == StatusVenda.FINALIZADA) venda.finalizar(); else venda.cancelar();
        assertThrows(IllegalStateException.class, () -> venda.adicionarItem(instrumento("B"), BigDecimal.ONE));
        assertThrows(IllegalStateException.class, () -> venda.removerItem(10L));
        assertThrows(IllegalStateException.class, venda::finalizar);
        assertThrows(IllegalStateException.class, venda::cancelar);
        assertEquals(estado, venda.getStatus());
        assertEquals(1, venda.getItens().size());
    }

    @Test
    void devePermitirCancelarVendaVazia() {
        var venda = venda();
        venda.cancelar();
        assertEquals(StatusVenda.CANCELADA, venda.getStatus());
    }

    @Test
    void itemDeveValidarPrecoEArredondarSubtotal() {
        var venda = venda();
        var instrumento = instrumento("A");
        assertThrows(IllegalArgumentException.class, () -> new ItemVenda(venda, instrumento, BigDecimal.ONE, null));
        assertThrows(IllegalArgumentException.class, () -> new ItemVenda(venda, instrumento, BigDecimal.ONE, new BigDecimal("-1")));
        assertEquals(new BigDecimal("0.00"), new ItemVenda(venda, instrumento, BigDecimal.ONE, BigDecimal.ZERO).calcularSubtotal());
        assertEquals(new BigDecimal("1.01"), new ItemVenda(venda, instrumento, BigDecimal.ONE, new BigDecimal("1.005")).calcularSubtotal());
    }
}
