package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.domain.Fornecedor;
import com.curso.instrumento2026.repository.CategoriaInstrumentoRepository;
import com.curso.instrumento2026.repository.FornecedorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InstrumentoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaInstrumentoRepository categoriaRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Test
    void deveCadastrarInstrumentoERetornar201() throws Exception {

        CategoriaInstrumento categoria =
                categoriaRepository.save(
                        new CategoriaInstrumento(
                                null,
                                "Cordas API"
                        )
                );

        Fornecedor fornecedor =
                fornecedorRepository.save(
                        new Fornecedor(
                                "Fornecedor API",
                                "12345678000199"
                        )
                );

        String json = """
                {
                    "codigoInstrumento": "API-001",
                    "nomeInstrumento": "Violino API",
                    "quantidadeEstoque": 10,
                    "precoUnitario": 2500.00,
                    "estoqueMinimo": 2.000,
                    "categoriaId": %d,
                    "fornecedorId": %d
                }
                """.formatted(
                categoria.getId(),
                fornecedor.getId()
        );

        mockMvc.perform(
                        post("/api/instrumentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(
                        jsonPath("$.codigoInstrumento")
                                .value("API-001")
                )
                .andExpect(
                        jsonPath("$.nomeInstrumento")
                                .value("Violino API")
                )
                .andExpect(
                        jsonPath("$.categoriaId")
                                .value(categoria.getId())
                )
                .andExpect(
                        jsonPath("$.fornecedorId")
                                .value(fornecedor.getId())
                );
    }

    @Test
    void deveRetornar400ParaInstrumentoInvalido()
            throws Exception {

        String json = """
                {
                    "codigoInstrumento": "",
                    "nomeInstrumento": "",
                    "quantidadeEstoque": -1,
                    "precoUnitario": -10,
                    "estoqueMinimo": -2,
                    "categoriaId": 0
                }
                """;

        mockMvc.perform(
                        post("/api/instrumentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.fields.codigoInstrumento")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.fields.nomeInstrumento")
                                .exists()
                );
    }

    @Test
    void deveRetornar404ParaInstrumentoInexistente()
            throws Exception {

        mockMvc.perform(
                        get("/api/instrumentos/999999999")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(
                        jsonPath("$.message")
                                .value("Instrumento não encontrado")
                );
    }

    @Test
    void deveListarInstrumentosERetornar200()
            throws Exception {

        mockMvc.perform(
                        get("/api/instrumentos")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveRetornar409ParaCodigoDuplicado()
            throws Exception {

        CategoriaInstrumento categoria =
                categoriaRepository.save(
                        new CategoriaInstrumento(
                                null,
                                "Sopro API"
                        )
                );

        String json = """
                {
                    "codigoInstrumento": "API-DUP-001",
                    "nomeInstrumento": "Flauta API",
                    "quantidadeEstoque": 5,
                    "precoUnitario": 1200.00,
                    "estoqueMinimo": 1.000,
                    "categoriaId": %d,
                    "fornecedorId": null
                }
                """.formatted(categoria.getId());

        mockMvc.perform(
                        post("/api/instrumentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/instrumentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}