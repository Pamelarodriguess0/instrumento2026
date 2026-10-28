package com.curso.instrumento2026.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {
    private Pessoa pessoa() {
        return new Pessoa("Ana", "ana@example.com", "52998224725", LocalDate.of(2026, 10, 22));
    }

    @Test
    void deveExigirPessoa() {
        assertThrows(NullPointerException.class, () -> new Cliente(null, "11999990000"));
    }

    @Test
    void deveNormalizarEAlterarTelefonePreservandoPessoa() {
        var pessoa = pessoa();
        var cliente = new Cliente(pessoa, " 11999990000 ");
        assertNull(cliente.getId());
        assertSame(pessoa, cliente.getPessoa());
        assertEquals("11999990000", cliente.getTelefone());
        cliente.alterarDados(" 11888880000 ");
        assertEquals("11888880000", cliente.getTelefone());
        assertSame(pessoa, cliente.getPessoa());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void devePermitirTelefoneOpcionalNaCriacaoEAlteracao(String telefone) {
        var cliente = new Cliente(pessoa(), telefone);
        assertNull(cliente.getTelefone());
        cliente.alterarDados("11999990000");
        cliente.alterarDados(telefone);
        assertNull(cliente.getTelefone());
    }

    @Test
    void mesmaPessoaPodeSerClienteEColaboradorSemDuplicarIdentidade() {
        var pessoa = pessoa();
        var cliente = new Cliente(pessoa, null);
        var colaborador = new Colaborador(pessoa, "VEN-01", FuncaoColaborador.VENDEDOR, LocalDate.now());
        pessoa.inativar();
        assertSame(pessoa, cliente.getPessoa());
        assertSame(pessoa, colaborador.getPessoa());
        assertEquals(Status.INATIVO, cliente.getPessoa().getStatus());
        assertEquals(Status.INATIVO, colaborador.getPessoa().getStatus());
        assertEquals("VEN-01", colaborador.getMatricula());
    }
}
