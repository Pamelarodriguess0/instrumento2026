package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.Pessoa;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.repository.ContaAcessoRepository;
import com.curso.instrumento2026.repository.PessoaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContaAcessoServiceUnitTest {
    private final ContaAcessoRepository contas = mock(ContaAcessoRepository.class);
    private final PessoaRepository pessoas = mock(PessoaRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final ContaAcessoService service = new ContaAcessoService(contas, pessoas, encoder);

    private DataIntegrityViolationException prepararFalha(String constraint, String sqlState) {
        when(pessoas.findById(1L)).thenReturn(Optional.of(new Pessoa("Ana", "ana@example.com", "52998224725", LocalDate.now())));
        when(encoder.encode(any())).thenReturn("hash-sintetico");
        var servidor = mock(ServerErrorMessage.class);
        when(servidor.getSQLState()).thenReturn(sqlState);
        when(servidor.getConstraint()).thenReturn(constraint);
        var erro = new DataIntegrityViolationException("Detalhes internos nao devem sair", new PSQLException(servidor));
        doThrow(erro).when(contas).saveAndFlush(any());
        return erro;
    }

    @ParameterizedTest
    @ValueSource(strings = {"uk_conta_acesso_login_lower", "uk_conta_acesso_pessoa"})
    void deveTraduzirConflitoDoBancoMesmoComPreConsultasNegativas(String constraint) {
        prepararFalha(constraint, "23505");
        var erro = assertThrows(RecursoDuplicadoException.class, () -> service.cadastrar(1L, "ANA.LOJA", "Senha Sintetica Longa"));
        assertEquals(constraint.endsWith("login_lower") ? "Login já cadastrado" : "Pessoa já possui conta de acesso", erro.getMessage());
        assertNull(erro.getCause());
        assertFalse(erro.toString().contains("hash-sintetico"));
        assertFalse(erro.toString().contains("Senha Sintetica Longa"));
        verify(contas).existsByLoginIgnoreCase("ana.loja");
        verify(contas).existsByPessoaId(1L);
        verify(contas).saveAndFlush(any());
    }

    @Test
    void naoDeveConfundirOutraConstraintOuOutroSqlStateComDuplicidade() {
        var erro = prepararFalha("outra_constraint", "23505");
        assertSame(erro, assertThrows(DataIntegrityViolationException.class,
                () -> service.cadastrar(1L, "ana.loja", "Senha Sintetica Longa")));
        erro = prepararFalha("uk_conta_acesso_pessoa", "23503");
        assertSame(erro, assertThrows(DataIntegrityViolationException.class,
                () -> service.cadastrar(1L, "ana.loja", "Senha Sintetica Longa")));
    }
}
