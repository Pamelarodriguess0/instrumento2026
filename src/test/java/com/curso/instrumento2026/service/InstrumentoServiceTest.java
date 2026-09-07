package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.domain.Status;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.InstrumentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InstrumentoServiceTest {

    @Autowired
    private InstrumentoService instrumentoService;

    @Autowired
    private CategoriaInstrumentoService categoriaService;

    @Autowired
    private InstrumentoRepository instrumentoRepository;

    @Test
    void deveCadastrarInstrumento() {

        CategoriaInstrumento categoria =
                new CategoriaInstrumento(
                        null,
                        "Percussão"
                );

        CategoriaInstrumento categoriaSalva =
                categoriaService.cadastrar(categoria);

        Instrumento instrumento = new Instrumento(
                null,
                "INT-TESTE-001",
                "Bateria",
                5,
                new BigDecimal("3500.00"),
                LocalDate.now(),
                Status.ATIVO,
                categoriaSalva
        );

        Instrumento instrumentoSalvo =
                instrumentoService.cadastrar(
                        instrumento,
                        categoriaSalva.getId(),
                        null
                );

        assertNotNull(instrumentoSalvo.getId());
    }

    @Test
    void naoDeveCadastrarInstrumentoComCategoriaInexistente() {

        CategoriaInstrumento categoriaInexistente =
                new CategoriaInstrumento(
                        Long.MAX_VALUE,
                        "Categoria inexistente"
                );

        Instrumento instrumento = new Instrumento(
                null,
                "INT-ROLLBACK-001",
                "Instrumento de teste",
                2,
                new BigDecimal("500.00"),
                LocalDate.now(),
                Status.ATIVO,
                categoriaInexistente
        );

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> instrumentoService.cadastrar(
                        instrumento,
                        Long.MAX_VALUE,
                        null
                )
        );

        assertFalse(
                instrumentoRepository.existsByCodigoInstrumento(
                        instrumento.getCodigoInstrumento()
                )
        );
    }
}