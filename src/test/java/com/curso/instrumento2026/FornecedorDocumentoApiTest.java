package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.Fornecedor;
import com.curso.instrumento2026.repository.FornecedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FornecedorDocumentoApiTest {
    @Autowired MockMvc mvc;
    @Autowired FornecedorRepository repository;

    @Test
    void deveCadastrarFornecedorComDocumentoValido() throws Exception {
        mvc.perform(post("/api/fornecedores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"razaoSocial\":\"Fornecedor fiscal\",\"cnpj\":\"11222333000181\"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.cnpj").value("11222333000181"));
        assertTrue(repository.existsByCnpj("11222333000181"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000182", "11111111111111", "11.222.333/0001-81"})
    void deveRetornarErroDeCampoSemPersistirDocumentoInvalido(String cnpj) throws Exception {
        long antes = repository.count();
        mvc.perform(post("/api/fornecedores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"razaoSocial\":\"Fornecedor fiscal\",\"cnpj\":\"" + cnpj + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields.cnpj").exists());
        assertEquals(antes, repository.count());
    }

    @Test
    void devePreservarMensagemDeObrigatoriedade() throws Exception {
        mvc.perform(post("/api/fornecedores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"razaoSocial\":\"Fornecedor fiscal\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.cnpj").value("CNPJ é obrigatório"));
    }

    @Test
    void devePreservarConflitoDeCnpjDuplicadoValido() throws Exception {
        repository.saveAndFlush(new Fornecedor("Fornecedor existente", "11222333000181"));
        mvc.perform(post("/api/fornecedores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"razaoSocial\":\"Fornecedor repetido\",\"cnpj\":\"11222333000181\"}"))
                .andExpect(status().isConflict());
    }
}
