package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Checkpoint8ApiTest {
    @Autowired MockMvc mvc;
    @Autowired CategoriaInstrumentoRepository categorias;
    @Autowired InstrumentoRepository instrumentos;
    @Autowired FornecedorRepository fornecedores;
    @Autowired EntityManager em;

    private CategoriaInstrumento categoria(String nome) {
        return categorias.save(new CategoriaInstrumento(null, "CP8 " + nome));
    }

    private Instrumento instrumento(CategoriaInstrumento categoria, String codigo, String nome,
                                    int saldo, String minimo, Fornecedor fornecedor) {
        var instrumento = new Instrumento(codigo, "CP8 " + nome, saldo, BigDecimal.TEN,
                new BigDecimal(minimo), LocalDate.of(2026, 1, 1));
        instrumento.definirCategoria(categoria);
        instrumento.definirFornecedor(fornecedor);
        categoria.adicionarInstrumento(instrumento);
        return instrumentos.save(instrumento);
    }

    private String atualizacao(Long categoriaId) {
        return """
                {"nomeInstrumento":"CP8 alterado","precoUnitario":25.90,
                 "estoqueMinimo":3,"categoriaId":%d,"fornecedorId":null}
                """.formatted(categoriaId);
    }

    @Test
    void deveAlterarCategoriaConsultarEAlternarStatus() throws Exception {
        var categoria = categoria("original");
        mvc.perform(put("/api/categorias/{id}", categoria.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"  CP8 renomeada  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("CP8 renomeada"));
        for (String estado : new String[]{"INATIVO", "ATIVO"}) {
            mvc.perform(put("/api/categorias/{id}/status", categoria.getId()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"" + estado + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(estado));
        }
        mvc.perform(get("/api/categorias/{id}", categoria.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("CP8 renomeada"));
    }

    @Test
    void deveAlterarInstrumentoSemModificarCamposControlados() throws Exception {
        var origem = categoria("origem");
        var destino = categoria("destino");
        var fornecedor = fornecedores.save(new Fornecedor("Fornecedor CP8", "34567890000199"));
        var instrumento = instrumento(origem, "CP8-ALTERAR", "original", 5, "0", fornecedor);
        mvc.perform(put("/api/instrumentos/{id}", instrumento.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(atualizacao(destino.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nomeInstrumento").value("CP8 alterado"))
                .andExpect(jsonPath("$.precoUnitario").value(25.90))
                .andExpect(jsonPath("$.categoriaId").value(destino.getId()))
                .andExpect(jsonPath("$.fornecedorId").isEmpty())
                .andExpect(jsonPath("$.codigoInstrumento").value("CP8-ALTERAR"))
                .andExpect(jsonPath("$.quantidadeEstoque").value(5))
                .andExpect(jsonPath("$.dataCadastro").value("2026-01-01"));
        em.flush();
        em.clear();
        mvc.perform(get("/api/instrumentos/{id}", instrumento.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoriaId").value(destino.getId()));
    }

    @Test
    void deveAlterarStatusEMovimentarEstoque() throws Exception {
        var instrumento = instrumento(categoria("estoque"), "CP8-EST", "estoque", 5, "0", null);
        for (String estado : new String[]{"INATIVO", "ATIVO"}) {
            mvc.perform(put("/api/instrumentos/{id}/status", instrumento.getId()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"" + estado + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(estado));
        }
        mvc.perform(post("/api/instrumentos/{id}/estoque/entradas", instrumento.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidadeEstoque").value(8));
        mvc.perform(post("/api/instrumentos/{id}/estoque/saidas", instrumento.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidadeEstoque").value(6));
    }

    @Test
    void deveExcluirSemCorpoERetornar404NaConsultaPosterior() throws Exception {
        var categoria = categoria("excluir");
        var instrumento = instrumento(categoria, "CP8-DEL", "excluir", 0, "0", null);
        mvc.perform(delete("/api/instrumentos/{id}", instrumento.getId()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get("/api/instrumentos/{id}", instrumento.getId())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/categorias/{id}", categoria.getId()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get("/api/categorias/{id}", categoria.getId())).andExpect(status().isNotFound());
    }

    @Test
    void devePesquisarCategoriasComFiltrosOrdenacaoEPaginas() throws Exception {
        categoria("pesquisa A");
        categoria("pesquisa B");
        var inativa = categoria("pesquisa C");
        inativa.inativar();
        categoria("outra");
        mvc.perform(get("/api/categorias").param("nome", " cp8 PESQUISA ").param("status", "ATIVO")
                .param("page", "1").param("size", "1").param("sort", "nome,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].nome").value("CP8 pesquisa B"))
                .andExpect(jsonPath("$.pagina").value(1)).andExpect(jsonPath("$.tamanho").value(1))
                .andExpect(jsonPath("$.totalElementos").value(2)).andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.primeira").value(false)).andExpect(jsonPath("$.ultima").value(true));
    }

    @Test
    void deveCombinarTodosOsFiltrosDeInstrumentoECarregarAssociacoes() throws Exception {
        var categoria = categoria("filtros");
        var outra = categoria("filtros outra");
        var fornecedor = fornecedores.save(new Fornecedor("Fornecedor CP8 filtros", "45678901000177"));
        var alvo = instrumento(categoria, "CP8-F1", "Violino filtros", 2, "3", fornecedor);
        instrumento(categoria, "CP8-F2", "Violino filtros", 3, "3", fornecedor);
        instrumento(categoria, "CP8-F3", "Violino filtros", 1, "3", null);
        instrumento(outra, "CP8-F4", "Violino filtros", 1, "3", fornecedor);
        instrumento(categoria, "CP8-F5", "Flauta filtros", 1, "3", fornecedor);
        instrumento(categoria, "CP8-F6", "Violino filtros", 1, "3", fornecedor).inativar();
        em.flush();
        em.clear();
        mvc.perform(get("/api/instrumentos").param("nomeInstrumento", " cp8 VIOLINO filtros ")
                .param("status", "ATIVO").param("categoriaId", categoria.getId().toString())
                .param("fornecedorId", fornecedor.getId().toString()).param("abaixoEstoqueMinimo", "true")
                .param("size", "5").param("sort", "nomeInstrumento,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(alvo.getId()))
                .andExpect(jsonPath("$.conteudo[0].categoriaNome").value("CP8 filtros"))
                .andExpect(jsonPath("$.conteudo[0].fornecedorRazaoSocial").value("Fornecedor CP8 filtros"))
                .andExpect(jsonPath("$.totalElementos").value(1)).andExpect(jsonPath("$.tamanho").value(5));
    }

    @Test
    void deveAplicarPadraoELimiteDePaginaNasDuasRotas() throws Exception {
        for (String rota : new String[]{"/api/categorias", "/api/instrumentos"}) {
            mvc.perform(get(rota)).andExpect(status().isOk()).andExpect(jsonPath("$.tamanho").value(20));
            mvc.perform(get(rota).param("size", "200"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.tamanho").value(100));
        }
    }

    @Test
    void deveValidarDtosAntesDeExecutarOperacoes() throws Exception {
        var instrumento = instrumento(categoria("validacao"), "CP8-VAL", "validacao", 5, "0", null);
        mvc.perform(put("/api/categorias/{id}", instrumento.getCategoria().getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.nome").exists());
        mvc.perform(put("/api/instrumentos/{id}", instrumento.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(atualizacao(instrumento.getCategoria().getId()).replace("25.90", "0")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.precoUnitario").exists());
        for (String rota : new String[]{"/api/categorias/" + instrumento.getCategoria().getId(),
                "/api/instrumentos/" + instrumento.getId()}) {
            mvc.perform(put(rota + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INVALIDO\"}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(put(rota + "/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }
        for (String operacao : new String[]{"entradas", "saidas"}) {
            mvc.perform(post("/api/instrumentos/{id}/estoque/" + operacao, instrumento.getId())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":0}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.quantidade").exists());
        }
        assertEquals(5, instrumento.getQuantidadeEstoque());
    }

    @Test
    void deveRetornar404ParaNovasOperacoesComIdAusente() throws Exception {
        var categoria = categoria("id ausente");
        mvc.perform(put("/api/instrumentos/{id}", Long.MAX_VALUE).contentType(MediaType.APPLICATION_JSON)
                .content(atualizacao(categoria.getId()))).andExpect(status().isNotFound());
        mvc.perform(put("/api/categorias/{id}", Long.MAX_VALUE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"CP8 ausente\"}")).andExpect(status().isNotFound());
        for (String rota : new String[]{"/api/categorias", "/api/instrumentos"}) {
            mvc.perform(delete(rota + "/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
            mvc.perform(put(rota + "/{id}/status", Long.MAX_VALUE).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"ATIVO\"}")).andExpect(status().isNotFound());
        }
        for (String operacao : new String[]{"entradas", "saidas"}) {
            mvc.perform(post("/api/instrumentos/{id}/estoque/" + operacao, Long.MAX_VALUE)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":1}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void deveRetornar409AoAtualizarNomeDuplicado() throws Exception {
        categoria("duplicada");
        var outra = categoria("duplicada outra");
        mvc.perform(put("/api/categorias/{id}", outra.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\" CP8 DUPLICADA \"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }
}
