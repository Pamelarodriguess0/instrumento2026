package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.exception.RecursoEmUsoException;
import com.curso.instrumento2026.repository.InstrumentoRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstrumentoExclusaoTest {
    private final InstrumentoRepository repository = mock(InstrumentoRepository.class);
    private final InstrumentoService service = new InstrumentoService(repository, null, null);

    private DataIntegrityViolationException falharExclusao(String constraint) {
        when(repository.findById(1L)).thenReturn(Optional.of(mock(Instrumento.class)));
        var erroServidor = mock(ServerErrorMessage.class);
        when(erroServidor.getSQLState()).thenReturn("23503");
        when(erroServidor.getConstraint()).thenReturn(constraint);
        var falha = new DataIntegrityViolationException("Integridade",
                new ConstraintViolationException("Restrição", new PSQLException(erroServidor), null));
        doThrow(falha).when(repository).flush();
        return falha;
    }

    @Test
    void deveConverterSomenteFkDoHistoricoDeVendas() {
        falharExclusao("fk_item_venda_instrumento");
        var erro = assertThrows(RecursoEmUsoException.class, () -> service.excluir(1L));
        assertEquals("Instrumento não pode ser excluído porque está vinculado a uma venda", erro.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"outra_fk", "uk_instrumento_codigo"})
    void devePreservarFalhasDeOutrasRestricoes(String constraint) {
        var falha = falharExclusao(constraint);
        assertSame(falha, assertThrows(DataIntegrityViolationException.class, () -> service.excluir(1L)));
    }

    @Test
    void devePreservarFalhaSemIdentificacaoDeConstraint() {
        when(repository.findById(1L)).thenReturn(Optional.of(mock(Instrumento.class)));
        var falha = new DataIntegrityViolationException("Outra falha de integridade");
        doThrow(falha).when(repository).flush();
        assertSame(falha, assertThrows(DataIntegrityViolationException.class, () -> service.excluir(1L)));
    }
}
