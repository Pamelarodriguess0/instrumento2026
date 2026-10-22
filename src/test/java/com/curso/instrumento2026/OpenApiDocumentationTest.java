
package com.curso.instrumento2026;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void deveDisponibilizarDocumentacaoOpenApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.openapi").exists());
    }

    @Test
    void deveConterInformacoesDaApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title")
                        .value("API de Controle de Instrumentos Musicais"))
                .andExpect(jsonPath("$.info.version")
                        .value("1.0.0"))
                .andExpect(jsonPath("$.info.contact.name")
                        .value("Curso de Spring Boot 2026"))
                .andExpect(jsonPath("$.externalDocs.url")
                        .value("https://github.com/jeffersonarpasserini/suporteos2026"));
    }

    @Test
    void deveDocumentarEndpointsPrincipais() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/instrumentos']").exists())
                .andExpect(jsonPath("$.paths['/api/categorias']").exists())
                .andExpect(jsonPath("$.paths['/api/fornecedores']").exists())
                .andExpect(jsonPath("$.paths['/api/health'].get").exists());
    }

    @Test
    void deveDocumentarRespostasDeErro() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/instrumentos'].post.responses['201']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/instrumentos'].post.responses['400']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/instrumentos'].post.responses['404']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/instrumentos'].post.responses['409']"
                ).exists());
    }

    @Test
    void deveDocumentarOperacoesDeInstrumentos() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/instrumentos'].get").exists())
                .andExpect(jsonPath("$.paths['/api/instrumentos'].post").exists())
                .andExpect(jsonPath("$.paths['/api/instrumentos/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/instrumentos/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/instrumentos/{id}'].delete").exists());
    }

    @Test
    void deveDocumentarOperacoesDeCategoriasEFornecedores() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/categorias'].get").exists())
                .andExpect(jsonPath("$.paths['/api/categorias'].post").exists())
                .andExpect(jsonPath("$.paths['/api/fornecedores'].get").exists())
                .andExpect(jsonPath("$.paths['/api/fornecedores'].post").exists());
    }

    @Test
    void deveDocumentarRespostasDeExclusao() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/instrumentos/{id}'].delete.responses['204']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/instrumentos/{id}'].delete.responses['404']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/categorias/{id}'].delete.responses['409']"
                ).exists());
    }

    @Test
    void deveDocumentarSchemaApiError() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ApiError").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError.properties.status").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError.properties.message").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError.properties.fields").exists());
    }

    @Test
    void deveDocumentarFiltrosDePesquisa() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/instrumentos'].get.parameters"
                ).isArray())
                .andExpect(jsonPath(
                        "$.paths['/api/categorias'].get.parameters"
                ).isArray());
    }

    @Test
    void deveDocumentarMovimentacoesDeEstoque() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/instrumentos/{id}/estoque/entradas'].post"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/instrumentos/{id}/estoque/saidas'].post"
                ).exists());
    }
}
