package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.repository.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Sem transação no teste: verifica mapeamento com OSIV desativado e rollback real do service.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Aula10Passo8ApiTest {
    @Autowired MockMvc mvc;
    @Autowired PessoaRepository pessoas;
    @Autowired ClienteRepository clientes;
    @Autowired ColaboradorRepository colaboradores;
    @Autowired InstrumentoRepository instrumentos;
    @Autowired CategoriaInstrumentoRepository categorias;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private TransactionTemplate tx;
    private final List<Long> pessoasCriadas = new ArrayList<>();
    private Long pessoaClienteId, pessoaVendedorId, clienteId, vendedorId, instrumentoId, segundoId, categoriaId;

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            var cliente = pessoas.save(new Pessoa("Cliente HTTP passo oito", "cliente.http@example.com", "52998224725", LocalDate.now()));
            var vendedor = pessoas.save(new Pessoa("Vendedor HTTP passo oito", "vendedor.http@example.com", "11144477735", LocalDate.now()));
            pessoaClienteId = cliente.getId();
            pessoaVendedorId = vendedor.getId();
            pessoasCriadas.add(pessoaClienteId);
            pessoasCriadas.add(pessoaVendedorId);
            clienteId = clientes.save(new Cliente(cliente, null)).getId();
            vendedorId = colaboradores.save(new Colaborador(vendedor, "HTTP-8", FuncaoColaborador.VENDEDOR, LocalDate.now())).getId();
            var categoria = categorias.save(new CategoriaInstrumento(null, "Categoria HTTP passo oito"));
            categoriaId = categoria.getId();
            instrumentoId = instrumento(categoria, "HTTP-8-A", 5);
            segundoId = instrumento(categoria, "HTTP-8-B", 1);
        });
    }

    private Long instrumento(CategoriaInstrumento categoria, String codigo, int saldo) {
        var instrumento = new Instrumento(codigo, "Violino HTTP", saldo, new BigDecimal("100.00"), BigDecimal.ZERO, LocalDate.now());
        instrumento.definirCategoria(categoria);
        return instrumentos.save(instrumento).getId();
    }

    @AfterEach
    void limparSomenteDadosDoTeste() {
        tx.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM venda WHERE cliente_id = ? OR vendedor_id = ?", clienteId, vendedorId);
            for (Long id : pessoasCriadas) {
                jdbc.update("DELETE FROM colaborador WHERE pessoa_id = ?", id);
                jdbc.update("DELETE FROM cliente WHERE pessoa_id = ?", id);
                jdbc.update("DELETE FROM pessoa WHERE id = ?", id);
            }
            jdbc.update("DELETE FROM instrumento WHERE id IN (?, ?)", instrumentoId, segundoId);
            jdbc.update("DELETE FROM categoria_instrumento WHERE id = ?", categoriaId);
        });
    }

    private Long abrir() throws Exception {
        var resultado = mvc.perform(post("/api/vendas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":%d,\"vendedorId\":%d}".formatted(clienteId, vendedorId)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.clienteNome").value("Cliente HTTP passo oito"))
                .andExpect(jsonPath("$.vendedorNome").value("Vendedor HTTP passo oito"))
                .andReturn();
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void adicionar(Long venda, Long instrumento, String quantidade) throws Exception {
        mvc.perform(post("/api/vendas/{id}/itens", venda).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instrumentoId\":%d,\"quantidade\":%s}".formatted(instrumento, quantidade)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens").isArray());
    }

    @Test
    void deveCadastrarPessoaValidarCpfENormalizarEmail() throws Exception {
        var resultado = mvc.perform(post("/api/pessoas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":" Nova pessoa ","email":"NOVA@EXAMPLE.COM","cpf":"01234567890"}
                                """))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Nova pessoa"))
                .andExpect(jsonPath("$.email").value("nova@example.com"))
                .andExpect(jsonPath("$.status").value("ATIVO")).andExpect(jsonPath("$.dataCadastro").exists()).andReturn();
        pessoasCriadas.add(((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue());
        mvc.perform(post("/api/pessoas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Inválida","email":"ok@example.com","cpf":"52998224724"}
                                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.cpf").exists());
    }

    @Test
    void deveAtualizarPessoaStatusEConsultar() throws Exception {
        mvc.perform(put("/api/pessoas/{id}", pessoaClienteId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Nome atualizado","email":"ATUALIZADO@EXAMPLE.COM","cpf":"52998224725"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("atualizado@example.com"));
        mvc.perform(put("/api/pessoas/{id}/status", pessoaClienteId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INATIVO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INATIVO"));
        mvc.perform(get("/api/pessoas/{id}", pessoaClienteId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Nome atualizado"));
    }

    @Test
    void deveRetornarConflitoDeCpfEEmail() throws Exception {
        for (String corpo : List.of(
                "{\"nome\":\"Outra\",\"email\":\"outra@example.com\",\"cpf\":\"52998224725\"}",
                "{\"nome\":\"Outra\",\"email\":\"CLIENTE.HTTP@EXAMPLE.COM\",\"cpf\":\"01234567890\"}")) {
            mvc.perform(post("/api/pessoas").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        }
    }

    @Test
    void deveCadastrarPapeisDaMesmaPessoaEPreservarIdentidadeNaAtualizacao() throws Exception {
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pessoaId\":%d,\"telefone\":\"123\"}".formatted(pessoaVendedorId)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.pessoaId").value(pessoaVendedorId))
                .andExpect(jsonPath("$.nome").value("Vendedor HTTP passo oito"));
        mvc.perform(post("/api/colaboradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pessoaId\":%d,\"matricula\":\"HTTP-8-NOVA\",\"funcao\":\"GERENTE\",\"dataAdmissao\":\"2026-01-01\"}".formatted(pessoaClienteId)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"));
        mvc.perform(put("/api/clientes/{id}", clienteId).contentType(MediaType.APPLICATION_JSON).content("{\"telefone\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.telefone").doesNotExist());
        mvc.perform(put("/api/colaboradores/{id}", vendedorId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matricula\":\"HTTP-8-ALTERADA\",\"funcao\":\"GERENTE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pessoaId").value(pessoaVendedorId));
        mvc.perform(get("/api/clientes/{id}", clienteId)).andExpect(status().isOk()).andExpect(jsonPath("$.cpf").value("52998224725"));
        mvc.perform(get("/api/colaboradores/{id}", vendedorId)).andExpect(status().isOk()).andExpect(jsonPath("$.funcao").value("GERENTE"));
    }

    @Test
    void deveRejeitarPapeisDuplicadosEPessoaInativa() throws Exception {
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content("{\"pessoaId\":%d}".formatted(pessoaClienteId)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/colaboradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pessoaId\":%d,\"matricula\":\"OUTRA\",\"funcao\":\"GERENTE\",\"dataAdmissao\":\"2026-01-01\"}".formatted(pessoaVendedorId)))
                .andExpect(status().isConflict());
        tx.executeWithoutResult(status -> pessoas.findById(pessoaVendedorId).orElseThrow().inativar());
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content("{\"pessoaId\":%d}".formatted(pessoaVendedorId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveValidarCamposObrigatoriosLimitesEEnumsDosDtos() throws Exception {
        mvc.perform(post("/api/pessoas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"invalido\",\"cpf\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.nome").exists())
                .andExpect(jsonPath("$.fields.email").exists()).andExpect(jsonPath("$.fields.cpf").exists());
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content("{\"pessoaId\":-1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.pessoaId").exists());
        mvc.perform(put("/api/clientes/{id}", clienteId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"" + "1".repeat(31) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.telefone").exists());
        mvc.perform(post("/api/colaboradores").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.dataAdmissao").exists());
        mvc.perform(put("/api/colaboradores/{id}", vendedorId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matricula\":\"X\",\"funcao\":\"INVALIDA\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/pessoas/{id}/status", pessoaClienteId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.status").exists());
        mvc.perform(post("/api/vendas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.clienteId").exists());
    }

    @Test
    void devePesquisarPessoasESeusPapeisComFiltrosEPaginacao() throws Exception {
        mvc.perform(get("/api/pessoas").param("nome", "CLIENTE HTTP").param("cpf", "52998224725").param("status", "ATIVO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1)).andExpect(jsonPath("$.tamanho").value(20));
        mvc.perform(get("/api/clientes").param("nome", "cliente http").param("cpf", "52998224725").param("size", "1000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1)).andExpect(jsonPath("$.tamanho").value(100));
        mvc.perform(get("/api/colaboradores").param("nome", "vendedor http").param("matricula", "HTTP-8").param("funcao", "VENDEDOR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo[0].id").value(vendedorId));
        mvc.perform(get("/api/colaboradores").param("funcao", "GERENTE").param("matricula", "HTTP-8"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void deveAdicionarConsultarERemoverItemSemBaixarEstoque() throws Exception {
        Long id = abrir();
        adicionar(id, instrumentoId, "2");
        var resultado = mvc.perform(get("/api/vendas/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(200))
                .andExpect(jsonPath("$.itens[0].instrumentoId").value(instrumentoId))
                .andExpect(jsonPath("$.itens[0].instrumentoNome").value("Violino HTTP"))
                .andExpect(jsonPath("$.itens[0].valorUnitario").value(100)).andReturn();
        Long itemId = ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.itens[0].id")).longValue();
        mvc.perform(delete("/api/vendas/{venda}/itens/{item}", id, itemId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens").isEmpty()).andExpect(jsonPath("$.total").value(0));
        assertEquals(5, instrumentos.findById(instrumentoId).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    void deveFinalizarComPrecoHistoricoERejeitarTransicoesPosteriores() throws Exception {
        Long id = abrir();
        adicionar(id, instrumentoId, "2");
        tx.executeWithoutResult(status -> instrumentos.findById(instrumentoId).orElseThrow().alterarPrecoUnitario(new BigDecimal("999")));
        mvc.perform(post("/api/vendas/{id}/finalizacao", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINALIZADA")).andExpect(jsonPath("$.total").value(200));
        for (String operacao : List.of("finalizacao", "cancelamento")) {
            mvc.perform(post("/api/vendas/{id}/{operacao}", id, operacao))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.fields").isMap());
        }
        assertEquals(3, instrumentos.findById(instrumentoId).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    void deveCancelarSemBaixaERejeitarAlteracao() throws Exception {
        Long id = abrir();
        adicionar(id, instrumentoId, "1");
        mvc.perform(post("/api/vendas/{id}/cancelamento", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));
        mvc.perform(post("/api/vendas/{id}/itens", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instrumentoId\":%d,\"quantidade\":1}".formatted(segundoId)))
                .andExpect(status().isBadRequest());
        assertEquals(5, instrumentos.findById(instrumentoId).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    void deveReverterTodasAsBaixasQuandoFaltarSaldo() throws Exception {
        Long id = abrir();
        adicionar(id, instrumentoId, "2");
        adicionar(id, segundoId, "2");
        mvc.perform(post("/api/vendas/{id}/finalizacao", id))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Estoque insuficiente"));
        assertEquals(5, instrumentos.findById(instrumentoId).orElseThrow().getQuantidadeEstoque());
        assertEquals(1, instrumentos.findById(segundoId).orElseThrow().getQuantidadeEstoque());
        mvc.perform(get("/api/vendas/{id}", id)).andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.5", "2147483648", "1.0001"})
    void deveRejeitarQuantidadeInvalida(String quantidade) throws Exception {
        Long id = abrir();
        mvc.perform(post("/api/vendas/{id}/itens", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instrumentoId\":%d,\"quantidade\":%s}".formatted(instrumentoId, quantidade)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/vendas/{id}", id)).andExpect(jsonPath("$.itens").isEmpty());
    }

    @Test
    void deveRejeitarItemDuplicadoVendaVaziaEParticipanteInvalido() throws Exception {
        Long id = abrir();
        mvc.perform(post("/api/vendas/{id}/finalizacao", id)).andExpect(status().isBadRequest());
        adicionar(id, instrumentoId, "1");
        mvc.perform(post("/api/vendas/{id}/itens", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instrumentoId\":%d,\"quantidade\":1}".formatted(instrumentoId)))
                .andExpect(status().isBadRequest());
        tx.executeWithoutResult(status -> colaboradores.findById(vendedorId).orElseThrow().alterarDados("HTTP-8", FuncaoColaborador.ESTOQUISTA));
        mvc.perform(post("/api/vendas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":%d,\"vendedorId\":%d}".formatted(clienteId, vendedorId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devePaginarResumosComFiltrosCombinadosSemItensNemTotal() throws Exception {
        Long primeira = abrir();
        adicionar(primeira, instrumentoId, "1");
        Long segunda = abrir();
        mvc.perform(post("/api/vendas/{id}/cancelamento", segunda)).andExpect(status().isOk());
        mvc.perform(get("/api/vendas").param("clienteId", clienteId.toString()).param("vendedorId", vendedorId.toString())
                        .param("status", "ABERTA").param("dataInicial", LocalDate.now().toString()).param("dataFinal", LocalDate.now().toString())
                        .param("size", "1").param("sort", "dataVenda,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(primeira))
                .andExpect(jsonPath("$.conteudo[0].itens").doesNotExist()).andExpect(jsonPath("$.conteudo[0].total").doesNotExist());
        mvc.perform(get("/api/vendas").param("clienteId", clienteId.toString()).param("size", "1").param("page", "1").param("sort", "id,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.conteudo[0].id").value(segunda)).andExpect(jsonPath("$.ultima").value(true));
        mvc.perform(get("/api/vendas").param("clienteId", clienteId.toString()).param("size", "500"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tamanho").value(100));
    }

    @Test
    void deveRetornar400ParaOrdenacaoInvalidaEPeriodoInvertido() throws Exception {
        for (String rota : List.of("pessoas", "clientes", "colaboradores", "vendas")) {
            mvc.perform(get("/api/" + rota).param("sort", "inexistente,asc"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
        mvc.perform(get("/api/vendas").param("dataInicial", "2026-10-22").param("dataFinal", "2026-10-21"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar404ParaRecursosAusentes() throws Exception {
        for (String rota : List.of("pessoas", "clientes", "colaboradores", "vendas")) {
            mvc.perform(get("/api/" + rota + "/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
        mvc.perform(post("/api/vendas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":%d,\"vendedorId\":%d}".formatted(Long.MAX_VALUE, vendedorId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveImpedirExcluirInstrumentoVendidoEPreservarHistorico() throws Exception {
        Long vendaId = abrir();
        adicionar(vendaId, instrumentoId, "2");
        var finalizada = mvc.perform(post("/api/vendas/{id}/finalizacao", vendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andReturn().getResponse().getContentAsString();
        Long itemId = ((Number) JsonPath.read(finalizada, "$.itens[0].id")).longValue();

        mvc.perform(delete("/api/instrumentos/{id}", instrumentoId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Instrumento não pode ser excluído porque está vinculado a uma venda"))
                .andExpect(jsonPath("$.path").value("/api/instrumentos/" + instrumentoId));

        mvc.perform(get("/api/instrumentos/{id}", instrumentoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(instrumentoId))
                .andExpect(jsonPath("$.quantidadeEstoque").value(3));
        mvc.perform(get("/api/vendas/{id}", vendaId))
                .andExpect(status().isOk())
                .andExpect(content().json(finalizada));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM item_venda WHERE id = ? AND instrumento_id = ?",
                Integer.class, itemId, instrumentoId));
    }

    @Test
    void devePublicarOperacoesEEsquemaDeErrosNoOpenApi() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/instrumentos/{id}'].delete.responses['409'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ApiError"))
                .andExpect(jsonPath("$.paths['/api/pessoas'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/clientes'].post.responses['409'].content['application/json'].schema['$ref']").value("#/components/schemas/ApiError"))
                .andExpect(jsonPath("$.paths['/api/colaboradores/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/vendas/{id}/itens'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/vendas/{id}/finalizacao'].post.responses['404']").exists())
                .andExpect(jsonPath("$.components.schemas.VendaResumoResponse.properties.itens").doesNotExist());
    }
}
