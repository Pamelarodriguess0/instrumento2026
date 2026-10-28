package com.curso.instrumento2026.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ColaboradorTest {
    private static final LocalDate DATA = LocalDate.of(2026, 10, 22);

    private Pessoa pessoa() {
        return new Pessoa("Ana", "ana@example.com", "52998224725", DATA);
    }

    @ParameterizedTest
    @EnumSource(FuncaoColaborador.class)
    void deveAceitarAsQuatroFuncoesDeNegocio(FuncaoColaborador funcao) {
        var pessoa = pessoa();
        var colaborador = new Colaborador(pessoa, " MAT-01 ", funcao, DATA);
        assertNull(colaborador.getId());
        assertSame(pessoa, colaborador.getPessoa());
        assertEquals("MAT-01", colaborador.getMatricula());
        assertEquals(funcao, colaborador.getFuncao());
        assertEquals(DATA, colaborador.getDataAdmissao());
    }

    @Test
    void deveExigirPessoaFuncaoEDataAdmissao() {
        assertThrows(NullPointerException.class, () -> new Colaborador(null, "MAT-01", FuncaoColaborador.GERENTE, DATA));
        assertThrows(NullPointerException.class, () -> new Colaborador(pessoa(), "MAT-01", null, DATA));
        assertThrows(NullPointerException.class, () -> new Colaborador(pessoa(), "MAT-01", FuncaoColaborador.GERENTE, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deveExigirMatriculaNaCriacaoEAlteracao(String matricula) {
        assertThrows(IllegalArgumentException.class, () -> new Colaborador(pessoa(), matricula, FuncaoColaborador.GERENTE, DATA));
        var colaborador = new Colaborador(pessoa(), "MAT-01", FuncaoColaborador.VENDEDOR, DATA);
        assertThrows(IllegalArgumentException.class, () -> colaborador.alterarDados(matricula, FuncaoColaborador.GERENTE));
    }

    @Test
    void deveAlterarSomenteMatriculaEFuncao() {
        var pessoa = pessoa();
        var colaborador = new Colaborador(pessoa, "MAT-01", FuncaoColaborador.VENDEDOR, DATA);
        colaborador.alterarDados(" MAT-02 ", FuncaoColaborador.GERENTE);
        assertEquals("MAT-02", colaborador.getMatricula());
        assertEquals(FuncaoColaborador.GERENTE, colaborador.getFuncao());
        assertSame(pessoa, colaborador.getPessoa());
        assertEquals(DATA, colaborador.getDataAdmissao());
    }

    @Test
    void deveRejeitarFuncaoNulaSemAlteracaoParcial() {
        var colaborador = new Colaborador(pessoa(), "MAT-01", FuncaoColaborador.VENDEDOR, DATA);
        assertThrows(NullPointerException.class, () -> colaborador.alterarDados("MAT-02", null));
        assertEquals("MAT-01", colaborador.getMatricula());
        assertEquals(FuncaoColaborador.VENDEDOR, colaborador.getFuncao());
    }
}
