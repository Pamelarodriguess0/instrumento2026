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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Checkpoint9ApiTest {
    @Autowired MockMvc mvc;
    @Autowired CategoriaInstrumentoRepository categorias;
    @Autowired InstrumentoRepository instrumentos;
    @Autowired EntityManager em;

    private Instrumento preparar() {
        var categoria = categorias.save(new CategoriaInstrumento(null, "CP9 Cordas"));
        var instrumento = new Instrumento("CP9-001", "Violino CP9", 5, BigDecimal.TEN,
                BigDecimal.ZERO, LocalDate.now());
        instrumento.definirCategoria(categoria);
        categoria.adicionarInstrumento(instrumento);
        instrumentos.saveAndFlush(instrumento);
        return instrumento;
    }

    private ResultActions erro(ResultActions resultado, int status, String tipo, String mensagem, String path) throws Exception {
        return resultado.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(tipo))
                .andExpect(jsonPath("$.message").value(mensagem))
                .andExpect(jsonPath("$.path").value(path));
    }

    @Test
    void deveRetornar400ParaSaldoInsuficienteSemAlterarEstoque() throws Exception {
        var instrumento = preparar();
        String path = "/api/instrumentos/" + instrumento.getId() + "/estoque/saidas";
        erro(mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":6}")),
                400, "Bad Request", "Estoque insuficiente", path).andExpect(jsonPath("$.fields").isEmpty());
        em.clear();
        assertEquals(5, instrumentos.findById(instrumento.getId()).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    void deveRetornar400ParaOrdenacaoInvalidaNasDuasPesquisas() throws Exception {
        for (String path : new String[]{"/api/categorias", "/api/instrumentos"}) {
            erro(mvc.perform(get(path).param("sort", "fornecedor.razaoSocial,asc")), 400, "Bad Request",
                    "Campo de ordenação inválido: fornecedor.razaoSocial", path)
                    .andExpect(jsonPath("$.fields").isEmpty());
        }
    }

    @Test
    void deveRetornar409ParaCategoriaEmUsoPreservandoRegistros() throws Exception {
        var instrumento = preparar();
        Long categoriaId = instrumento.getCategoria().getId();
        String path = "/api/categorias/" + categoriaId;
        erro(mvc.perform(delete(path)), 409, "Conflict",
                "Categoria não pode ser excluída porque possui instrumentos", path)
                .andExpect(jsonPath("$.fields").isEmpty());
        em.clear();
        assertTrue(categorias.existsById(categoriaId));
        assertEquals(categoriaId, instrumentos.findById(instrumento.getId()).orElseThrow().getCategoria().getId());
    }

    @Test
    void deveRetornar400ParaFracoesSemTruncarSaldo() throws Exception {
        var instrumento = preparar();
        for (String operacao : new String[]{"entradas", "saidas"}) {
            String path = "/api/instrumentos/" + instrumento.getId() + "/estoque/" + operacao;
            erro(mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":1.5}")),
                    400, "Bad Request", "Quantidade deve ser inteira e estar no intervalo permitido", path);
        }
        em.clear();
        assertEquals(5, instrumentos.findById(instrumento.getId()).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    void deveRetornar400ParaOverflowSemAlterarSaldo() throws Exception {
        var instrumento = preparar();
        String path = "/api/instrumentos/" + instrumento.getId() + "/estoque/entradas";
        erro(mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":2147483647}")),
                400, "Bad Request", "Quantidade em estoque excede o limite permitido", path);
        em.clear();
        assertEquals(5, instrumentos.findById(instrumento.getId()).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    void devePreservarHandlerDeValidacaoComCampos() throws Exception {
        var instrumento = preparar();
        String path = "/api/instrumentos/" + instrumento.getId() + "/estoque/entradas";
        for (String json : new String[]{"{}", "{\"quantidade\":0}", "{\"quantidade\":-1}"}) {
            erro(mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json)),
                    400, "Bad Request", "Um ou mais campos são inválidos", path)
                    .andExpect(jsonPath("$.fields.quantidade").exists());
        }
    }

    @Test
    void devePreservarHandlerDeJsonInvalido() throws Exception {
        var instrumento = preparar();
        String path = "/api/instrumentos/" + instrumento.getId() + "/status";
        for (String json : new String[]{"{", "{\"status\":\"DESCONHECIDO\"}"}) {
            erro(mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(json)),
                    400, "Bad Request", "JSON inválido ou malformado", path);
        }
    }

    @Test
    void devePreservarHandlerDeRecursoNaoEncontrado() throws Exception {
        String path = "/api/instrumentos/" + Long.MAX_VALUE;
        erro(mvc.perform(get(path)), 404, "Not Found", "Instrumento não encontrado", path);
    }

    @Test
    void devePreservarHandlerDeDuplicidade() throws Exception {
        preparar();
        var outra = categorias.save(new CategoriaInstrumento(null, "CP9 Outra"));
        String path = "/api/categorias/" + outra.getId();
        erro(mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"CP9 CORDAS\"}")), 409, "Conflict", "Nome da categoria já cadastrado", path);
    }

    @Test
    void deveManterExclusaoConcluidaComo204SemCorpo() throws Exception {
        var instrumento = preparar();
        mvc.perform(delete("/api/instrumentos/{id}", instrumento.getId()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(delete("/api/categorias/{id}", instrumento.getCategoria().getId()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
    }
}
