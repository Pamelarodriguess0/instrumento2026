package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoriaInstrumentoServiceTest {

    @Autowired
    private CategoriaInstrumentoService service;

    @Test
    void deveCadastrarCategoria() {

        CategoriaInstrumento categoria =
                new CategoriaInstrumento(null, "Teclas");

        CategoriaInstrumento categoriaSalva =
                service.cadastrar(categoria);

        assertNotNull(categoriaSalva.getId());
    }
}