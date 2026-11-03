
package com.curso.instrumento2026;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.jayway.jsonpath.JsonPath;
import io.swagger.v3.core.util.Yaml;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
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

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "/api/pessoas|post|201,400,409|Pessoas",
            "/api/pessoas|get|200,400|Pessoas",
            "/api/pessoas/{id}|get|200,404|Pessoas",
            "/api/pessoas/{id}|put|200,400,404,409|Pessoas",
            "/api/pessoas/{id}/status|put|200,400,404|Pessoas",
            "/api/clientes|post|201,400,404,409|Clientes",
            "/api/clientes|get|200,400|Clientes",
            "/api/clientes/{id}|get|200,404|Clientes",
            "/api/clientes/{id}|put|200,400,404|Clientes",
            "/api/colaboradores|post|201,400,404,409|Colaboradores",
            "/api/colaboradores|get|200,400|Colaboradores",
            "/api/colaboradores/{id}|get|200,404|Colaboradores",
            "/api/colaboradores/{id}|put|200,400,404,409|Colaboradores",
            "/api/vendas|post|201,400,404|Vendas",
            "/api/vendas|get|200,400|Vendas",
            "/api/vendas/{id}|get|200,404|Vendas",
            "/api/vendas/{id}/itens|post|200,400,404|Vendas",
            "/api/vendas/{vendaId}/itens/{itemId}|delete|200,400,404|Vendas",
            "/api/vendas/{id}/finalizacao|post|200,400,404|Vendas",
            "/api/vendas/{id}/cancelamento|post|200,400,404|Vendas"
    })
    void deveDocumentarTodasAsOperacoesDaAula10ComRespostasCoerentes(
            String rota, String metodo, String codigos, String tag) throws Exception {
        var documento = JsonPath.parse(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String operacao = "$.paths['" + rota + "']." + metodo;
        assertFalse(documento.read(operacao + ".summary", String.class).isBlank());
        assertEquals(List.of(tag), documento.read(operacao + ".tags"));
        Map<String, Object> respostas = documento.read(operacao + ".responses");
        assertEquals(Set.copyOf(Arrays.asList(codigos.split(","))), respostas.keySet());
        for (String codigo : respostas.keySet()) {
            String resposta = operacao + ".responses['" + codigo + "']";
            if (codigo.startsWith("4")) {
                assertEquals("#/components/schemas/ApiError", documento.read(
                        resposta + ".content['application/json'].schema['$ref']"));
            } else {
                assertNotNull(documento.read(resposta + ".content['*/*'].schema"));
            }
        }
        if (respostas.containsKey("201")) {
            assertNotNull(documento.read(operacao + ".responses['201'].headers.Location"));
        }
    }

    @Test
    void deveDocumentarDetalheDoClienteEVendaComSchemasCompletos() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/clientes/{id}'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ClienteResponse"))
                .andExpect(jsonPath("$.paths['/api/vendas/{id}'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/VendaResponse"))
                .andExpect(jsonPath("$.components.schemas.VendaResponse.properties.itens.items['$ref']")
                        .value("#/components/schemas/ItemVendaResponse"))
                .andExpect(jsonPath("$.components.schemas.VendaResponse.properties.total.type").value("number"))
                .andExpect(jsonPath("$.components.schemas.VendaResponse.properties.clienteId").exists())
                .andExpect(jsonPath("$.components.schemas.VendaResponse.properties.vendedorId").exists())
                .andExpect(jsonPath("$.components.schemas.VendaResponse.properties.dataVenda").exists())
                .andExpect(jsonPath("$.components.schemas.VendaResponse.properties.status").exists())
                .andExpect(jsonPath("$.components.schemas.VendaResumoResponse.properties.itens").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.VendaResumoResponse.properties.total").doesNotExist());
    }

    @Test
    void deveExplicitarQuantidadeInteiraPositivaNoContratoSemAlterarValidacao() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ItemVendaRequest.properties.quantidade.type").value("integer"))
                .andExpect(jsonPath("$.components.schemas.ItemVendaRequest.properties.quantidade.format").value("int32"))
                .andExpect(jsonPath("$.components.schemas.ItemVendaRequest.properties.quantidade.minimum").value(1))
                .andExpect(jsonPath("$.components.schemas.ItemVendaRequest.properties.quantidade.maximum").value(2147483647))
                .andExpect(jsonPath("$.components.schemas.ItemVendaRequest.properties.quantidade.multipleOf").value(1));
    }

    @ParameterizedTest
    @CsvSource({"pessoas,nome cpf status", "clientes,nome cpf", "colaboradores,nome matricula funcao",
            "vendas,status clienteId vendedorId dataInicial dataFinal"})
    void deveDocumentarFiltrosEPaginacaoDaAula10(String recurso, String filtros) throws Exception {
        var documento = JsonPath.parse(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        List<Map<String, Object>> parametros = documento.read("$.paths['/api/" + recurso + "'].get.parameters");
        Set<Object> nomes = parametros.stream().map(p -> p.get("name")).collect(Collectors.toSet());
        for (String nome : (filtros + " page size sort").split(" ")) assertTrue(nomes.contains(nome), nome);
    }

    @ParameterizedTest
    @CsvSource({"PessoaRequest", "PessoaResponse", "ClienteRequest", "ClienteAtualizacaoRequest", "ClienteResponse",
            "ColaboradorRequest", "ColaboradorAtualizacaoRequest", "ColaboradorResponse", "VendaRequest",
            "VendaResponse", "VendaResumoResponse", "ItemVendaRequest", "ItemVendaResponse"})
    void deveOferecerExemplosNosNovosDtos(String schema) throws Exception {
        var documento = JsonPath.parse(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Map<String, Map<String, Object>> propriedades = documento.read("$.components.schemas." + schema + ".properties");
        propriedades.forEach((campo, propriedade) -> {
            if (!campo.equals("itens")) assertNotNull(propriedade.get("example"), schema + "." + campo);
        });
    }

    @Test
    void devePublicarYamlEquivalenteAoJson() throws Exception {
        String yaml = mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        Map<?, ?> exportado = Yaml.mapper().readValue(yaml, Map.class);
        assertNotNull(exportado.get("paths"));
        assertNotNull(exportado.get("components"));
        String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(Yaml.mapper().readTree(json).get("paths"), Yaml.mapper().readTree(yaml).get("paths"));
        assertEquals(Yaml.mapper().readTree(json).get("components"), Yaml.mapper().readTree(yaml).get("components"));
        if (Boolean.getBoolean("exportOpenApi")) {
            Files.writeString(Path.of("target/aula10-openapi.yaml"), yaml);
        }
    }

    @Test
    void exportacaoDaAula10DevePreservarContratoComAdicoesExplicitasDaAula12() throws Exception {
        String yaml = mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var publicado = Yaml.mapper().readTree(yaml);
        var exportado = Yaml.mapper().readTree(Files.readString(Path.of("docs/aula10/openapi.yaml")));
        assertEquals(Yaml.mapper().readTree("[{\"basicAuth\":[]}]"), publicado.get("security"));
        var esquemasSeguranca = ((com.fasterxml.jackson.databind.node.ObjectNode) publicado.get("components"))
                .remove("securitySchemes");
        assertEquals(Yaml.mapper().readTree("{\"basicAuth\":{\"type\":\"http\",\"scheme\":\"basic\"}}"), esquemasSeguranca);
        // As duas operações históricas públicas retiram explicitamente a exigência global.
        for (var publica : Map.of("/api/health", "get", "/api/pessoas", "post").entrySet()) {
            var operacao = (com.fasterxml.jackson.databind.node.ObjectNode) publicado.get("paths")
                    .get(publica.getKey()).get(publica.getValue());
            assertEquals(Yaml.mapper().readTree("[]"), operacao.remove("security"), publica.getKey());
        }
        // A Aula 11 altera somente estas três descrições do contrato histórico.
        for (String rota : List.of("/api/vendas/{id}/finalizacao",
                "/api/instrumentos/{id}/estoque/entradas", "/api/instrumentos/{id}/estoque/saidas")) {
            var operacao = publicado.get("paths").get(rota).get("post");
            String descricao = operacao.get("description").asText();
            assertTrue(descricao.contains("bloqueio pessimista"), rota);
            assertTrue(descricao.contains("transação"), rota);
            if (rota.equals("/api/vendas/{id}/finalizacao")) {
                assertTrue(descricao.contains("ordem crescente de ID"));
                assertTrue(descricao.contains("evitando que duas vendas consumam o mesmo estoque"));
            } else {
                assertTrue(descricao.contains("quantidade inteira positiva"));
                assertTrue(descricao.contains("Repetir a chamada repete o efeito"));
                if (rota.endsWith("/saidas")) assertTrue(descricao.contains("sem permitir saldo negativo"));
            }
            ((com.fasterxml.jackson.databind.node.ObjectNode) operacao).put("description",
                    exportado.get("paths").get(rota).get("post").get("description").asText());
        }
        // Remover somente adições explicitamente conhecidas da cópia em memória.
        // A igualdade integral restante protege métodos, operationId, parâmetros, respostas e schemas antigos.
        var caminhos = (com.fasterxml.jackson.databind.node.ObjectNode) publicado.get("paths");
        for (var novaOperacao : Map.of("/api/contas-acesso", "post",
                "/api/contas-acesso/me", "get", "/api/contas-acesso/me/senha", "put").entrySet()) {
            assertFalse(exportado.get("paths").has(novaOperacao.getKey()));
            var caminho = caminhos.remove(novaOperacao.getKey());
            assertNotNull(caminho, novaOperacao.getKey());
            assertEquals(1, caminho.size(), novaOperacao.getKey());
            assertTrue(caminho.has(novaOperacao.getValue()), novaOperacao.getKey());
        }
        var schemas = (com.fasterxml.jackson.databind.node.ObjectNode) publicado.get("components").get("schemas");
        for (String novoSchema : List.of("ContaAcessoRequest", "ContaAcessoResponse", "AlterarSenhaContaRequest")) {
            assertFalse(exportado.get("components").get("schemas").has(novoSchema));
            assertNotNull(schemas.remove(novoSchema), novoSchema);
        }
        assertEquals(exportado.get("paths"), caminhos);
        assertEquals(exportado.get("components"), publicado.get("components"));
        assertEquals(publicado.get("info"), exportado.get("info"));
    }

    @Test
    void devePublicarBasicGlobalComExcecoesSomenteNasTresOperacoesPublicas() throws Exception {
        var resposta = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var documento = Yaml.mapper().readTree(resposta.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var basic = Yaml.mapper().readTree("[{\"basicAuth\":[]}]");
        assertEquals(basic, documento.get("security"));
        assertEquals(Yaml.mapper().readTree("{\"basicAuth\":{\"type\":\"http\",\"scheme\":\"basic\"}}"),
                documento.get("components").get("securitySchemes"));
        Set<String> publicas = Set.of("get /api/health", "post /api/pessoas", "post /api/contas-acesso");
        Set<String> encontradas = new java.util.HashSet<>();
        documento.get("paths").fields().forEachRemaining(rota -> rota.getValue().fields().forEachRemaining(metodo -> {
            String operacao = metodo.getKey() + " " + rota.getKey();
            var seguranca = metodo.getValue().has("security") ? metodo.getValue().get("security") : documento.get("security");
            if (publicas.contains(operacao)) {
                assertNotNull(metodo.getValue().get("security"), operacao);
                assertTrue(seguranca.isArray() && seguranca.isEmpty(), operacao);
                encontradas.add(operacao);
            } else {
                assertEquals(basic, seguranca, operacao);
            }
        }));
        assertEquals(publicas, encontradas);
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "/api/contas-acesso|post|ContaAcessoRequest|201|ContaAcessoResponse|201,400,404,409",
            "/api/contas-acesso/me|get|nenhum|200|ContaAcessoResponse|200,401,404",
            "/api/contas-acesso/me/senha|put|AlterarSenhaContaRequest|204|nenhum|204,400,401,404"
    })
    void deveDocumentarOperacoesDeContaComDtosECodigosReais(String rota, String metodo, String entrada,
                                                         String sucesso, String saida, String codigos) throws Exception {
        var resposta = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var documento = Yaml.mapper().readTree(resposta.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var operacao = documento.get("paths").get(rota).get(metodo);
        assertFalse(operacao.get("description").asText().isBlank());
        Set<String> retornos = new java.util.HashSet<>();
        operacao.get("responses").fieldNames().forEachRemaining(retornos::add);
        assertEquals(Set.of(codigos.split(",")), retornos);
        if (entrada.equals("nenhum")) {
            assertFalse(operacao.has("requestBody"));
        } else {
            assertEquals("#/components/schemas/" + entrada,
                    operacao.at("/requestBody/content/application~1json/schema/$ref").asText());
            assertTrue(operacao.at("/requestBody/required").asBoolean());
        }
        if (saida.equals("nenhum")) {
            assertFalse(operacao.get("responses").get(sucesso).has("content"));
        } else {
            assertEquals("#/components/schemas/" + saida,
                    operacao.get("responses").get(sucesso).at("/content/application~1json/schema/$ref").asText());
        }
        for (String codigo : retornos) {
            var retorno = operacao.get("responses").get(codigo);
            if (codigo.equals("401")) {
                assertFalse(retorno.has("content"));
                assertTrue(retorno.get("description").asText().contains("filtros de segurança"));
            } else if (Set.of("400", "404", "409").contains(codigo)) {
                assertEquals("#/components/schemas/ApiError", retorno.at("/content/application~1json/schema/$ref").asText());
            }
        }
    }

    @Test
    void schemasDeContaDevemManterCredenciaisSomenteNaEntrada() throws Exception {
        var resposta = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var documento = Yaml.mapper().readTree(resposta.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var schemas = documento.get("components").get("schemas");
        Set<String> propriedades = new java.util.HashSet<>();
        schemas.get("ContaAcessoResponse").get("properties").fieldNames().forEachRemaining(propriedades::add);
        assertEquals(Set.of("id", "pessoaId", "nomePessoa", "login", "status", "perfis"), propriedades);
        assertFalse(schemas.toString().contains("senhaHash"));
        assertFalse(schemas.toString().contains("senha_hash"));
        for (var entrada : Map.of("ContaAcessoRequest", List.of("senha"),
                "AlterarSenhaContaRequest", List.of("senhaAtual", "novaSenha")).entrySet()) {
            for (String campo : entrada.getValue()) {
                var propriedade = schemas.get(entrada.getKey()).get("properties").get(campo);
                assertTrue(propriedade.get("writeOnly").asBoolean(), campo);
                assertEquals("password", propriedade.get("format").asText());
            }
        }
    }

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
