
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
    void exportacaoDaAula10DeveCorresponderAoContratoPublicado() throws Exception {
        String yaml = mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var publicado = Yaml.mapper().readTree(yaml);
        var exportado = Yaml.mapper().readTree(Files.readString(Path.of("docs/aula10/openapi.yaml")));
        assertEquals(publicado.get("paths"), exportado.get("paths"));
        assertEquals(publicado.get("components"), exportado.get("components"));
        assertEquals(publicado.get("info"), exportado.get("info"));
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
