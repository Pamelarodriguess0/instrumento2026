package com.curso.instrumento2026.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class PessoaTest {
    private static final LocalDate DATA = LocalDate.of(2026, 10, 22);

    @Test
    void deveCriarPessoaNormalizadaEAtiva() {
        var pessoa = new Pessoa("  Ana Souza  ", "  ANA@EXAMPLE.COM  ", "52998224725", DATA);
        assertNull(pessoa.getId());
        assertEquals("Ana Souza", pessoa.getNome());
        assertEquals("ana@example.com", pessoa.getEmail());
        assertEquals("52998224725", pessoa.getCpf());
        assertEquals(DATA, pessoa.getDataCadastro());
        assertEquals(Status.ATIVO, pessoa.getStatus());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deveRejeitarNomeObrigatorioNaCriacaoEAlteracao(String nome) {
        assertThrows(IllegalArgumentException.class, () -> new Pessoa(nome, "a@b.com", "52998224725", DATA));
        var pessoa = nova();
        assertThrows(IllegalArgumentException.class, () -> pessoa.alterarDados(nome, "a@b.com", "52998224725"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "ana", "ana@dominio", "a b@dominio.com", "a@@dominio.com"})
    void deveRejeitarEmailInvalidoNaCriacaoEAlteracao(String email) {
        assertThrows(IllegalArgumentException.class, () -> new Pessoa("Ana", email, "52998224725", DATA));
        var pessoa = nova();
        assertThrows(IllegalArgumentException.class, () -> pessoa.alterarDados("Ana", email, "52998224725"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "52998224724", "11111111111", "529.982.247-25", "5299822472A"})
    void deveRejeitarCpfInvalidoNaCriacaoEAlteracao(String cpf) {
        assertThrows(IllegalArgumentException.class, () -> new Pessoa("Ana", "a@b.com", cpf, DATA));
        var pessoa = nova();
        assertThrows(IllegalArgumentException.class, () -> pessoa.alterarDados("Ana", "a@b.com", cpf));
    }

    @Test
    void deveExigirDataCadastro() {
        assertThrows(NullPointerException.class, () -> new Pessoa("Ana", "a@b.com", "52998224725", null));
    }

    @Test
    void deveAlterarDadosSemModificarDataOuStatus() {
        var pessoa = nova();
        pessoa.inativar();
        pessoa.alterarDados("  Bruno  ", " BRUNO@EXAMPLE.COM ", "11144477735");
        assertEquals("Bruno", pessoa.getNome());
        assertEquals("bruno@example.com", pessoa.getEmail());
        assertEquals("11144477735", pessoa.getCpf());
        assertEquals(DATA, pessoa.getDataCadastro());
        assertEquals(Status.INATIVO, pessoa.getStatus());
        pessoa.ativar();
        assertEquals(Status.ATIVO, pessoa.getStatus());
    }

    @Test
    void alteracaoRejeitadaNaoDeveDeixarDadosParciais() {
        var pessoa = nova();
        assertThrows(IllegalArgumentException.class, () -> pessoa.alterarDados("Bruno", "b@c.com", "52998224724"));
        assertEquals("Ana", pessoa.getNome());
        assertEquals("ana@example.com", pessoa.getEmail());
        assertEquals("52998224725", pessoa.getCpf());
    }

    private Pessoa nova() {
        return new Pessoa("Ana", "ana@example.com", "52998224725", DATA);
    }
}
