
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import com.curso.instrumento2026.api.*;
import com.curso.instrumento2026.api.mapper.*;
import com.curso.instrumento2026.service.*;
import com.curso.instrumento2026.config.*;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AutenticacaoController.class, HealthController.class, ContaAcessoController.class,
        PessoaController.class, ClienteController.class, ColaboradorController.class, CategoriaInstrumentoController.class,
        FornecedorController.class, InstrumentoController.class, VendaController.class}, properties = {
        "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true",
        "app.security.jwt.issuer=https://instrumento2026.local", "app.security.jwt.expiration=PT30M",
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.initial-admin.enabled=false"})
@Import({OpenApiConfig.class, SecurityConfig.class, JwtConfig.class, PasswordEncoderConfig.class,
        OpenApiDocumentationTest.ContextoSemBanco.class})
@ImportAutoConfiguration({org.springdoc.core.configuration.SpringDocConfiguration.class,
        org.springdoc.core.properties.SpringDocConfigProperties.class,
        org.springdoc.core.configuration.SpringDocSecurityConfiguration.class,
        org.springdoc.core.configuration.SpringDocPageableConfiguration.class,
        org.springdoc.core.configuration.SpringDocSortConfiguration.class,
        org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration.class,
        org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration.class,
        org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration.class,
        org.springdoc.webmvc.ui.SwaggerConfig.class,
        org.springdoc.core.properties.SwaggerUiConfigProperties.class,
        org.springdoc.core.properties.SwaggerUiOAuthProperties.class})
@MockitoBean(types = {AutenticacaoService.class, ContaAcessoService.class, PessoaService.class, ClienteService.class,
        ColaboradorService.class, CategoriaInstrumentoService.class, FornecedorService.class, InstrumentoService.class,
        VendaService.class, ContaAcessoMapper.class, PessoaMapper.class, ClienteMapper.class, ColaboradorMapper.class,
        CategoriaInstrumentoMapper.class, FornecedorMapper.class, InstrumentoMapper.class, VendaMapper.class})
class OpenApiDocumentationTest {

    @Autowired
    MockMvc mvc;
    @Autowired ApplicationContext contexto;
    @BeforeEach void contextoNaoDeveAcessarBanco() {
        assertTrue(contexto.getBeansOfType(javax.sql.DataSource.class).isEmpty());
    }

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
        var esperados = new java.util.HashSet<>(Arrays.asList(codigos.split(",")));
        esperados.addAll(Set.of("401", "403"));
        assertEquals(esperados, respostas.keySet());
        for (String codigo : respostas.keySet()) {
            String resposta = operacao + ".responses['" + codigo + "']";
            if (Set.of("401", "403").contains(codigo)) {
                assertFalse(((Map<?, ?>) respostas.get(codigo)).containsKey("content"));
            } else if (codigo.startsWith("4")) {
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
    void exportacaoDaAula10DevePreservarContratoComAdicoesExplicitasAteAula14() throws Exception {
        String yaml = mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        com.curso.instrumento2026.support.ContratoOpenApiSupport.verificarCompatibilidadeComAula10(Yaml.mapper().readTree(yaml));
    }

    @Test
    void devePublicarBearerGlobalComExcecoesSomenteEmHealthELogin() throws Exception {
        var resposta = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var documento = Yaml.mapper().readTree(resposta.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var bearer = Yaml.mapper().readTree("[{\"bearerAuth\":[]}]");
        assertEquals(bearer, documento.get("security"));
        assertEquals(Yaml.mapper().readTree("{\"bearerAuth\":{\"type\":\"http\",\"scheme\":\"bearer\",\"bearerFormat\":\"JWT\"}}"),
                documento.get("components").get("securitySchemes"));
        Set<String> publicas = Set.of("get /api/health", "post /api/autenticacao/login");
        Set<String> encontradas = new java.util.HashSet<>();
        documento.get("paths").fields().forEachRemaining(rota -> rota.getValue().fields().forEachRemaining(metodo -> {
            String operacao = metodo.getKey() + " " + rota.getKey();
            var seguranca = metodo.getValue().has("security") ? metodo.getValue().get("security") : documento.get("security");
            if (publicas.contains(operacao)) {
                assertNotNull(metodo.getValue().get("security"), operacao);
                assertTrue(seguranca.isArray() && seguranca.isEmpty(), operacao);
                encontradas.add(operacao);
            } else {
                assertEquals(bearer, seguranca, operacao);
                boolean administrativa = metodo.getKey().equals("delete")
                        || operacao.equals("post /api/contas-acesso")
                        || rota.getKey().matches("/api/contas-acesso/[^/]+/(perfis|status)");
                String perfil = administrativa ? "ADMINISTRADOR" : "OPERADOR ou ADMINISTRADOR";
                assertTrue(metodo.getValue().path("description").asText().endsWith("perfil " + perfil + "."), operacao);
                for (String codigo : List.of("401", "403")) {
                    var erro = metodo.getValue().path("responses").get(codigo);
                    assertNotNull(erro, operacao + " " + codigo);
                    assertFalse(erro.has("content"), operacao);
                    assertTrue(erro.path("description").asText().contains("filtros"), operacao);
                }
            }
        }));
        assertEquals(publicas, encontradas);
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "/api/contas-acesso|post|ContaAcessoRequest|201|ContaAcessoResponse|201,400,401,403,404,409",
            "/api/contas-acesso/me|get|nenhum|200|ContaAcessoResponse|200,401,403,404",
            "/api/contas-acesso/me/senha|put|AlterarSenhaContaRequest|204|nenhum|204,400,401,403,404",
            "/api/contas-acesso/{id}/perfis|put|PerfisContaAcessoRequest|200|ContaAcessoResponse|200,400,401,403,404",
            "/api/contas-acesso/{id}/status|put|StatusContaAcessoRequest|200|ContaAcessoResponse|200,400,401,403,404"
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
            if (Set.of("401", "403").contains(codigo)) {
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

    @Test
    void deveDocumentarLoginPublicoComDtosECodigosEfetivos() throws Exception {
        var resposta = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var documento = Yaml.mapper().readTree(resposta.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var login = documento.at("/paths/~1api~1autenticacao~1login/post");
        assertEquals(Yaml.mapper().readTree("[]"), login.get("security"));
        assertFalse(login.get("summary").asText().isBlank());
        assertTrue(login.get("description").asText().contains("30 minutos"));
        assertEquals("#/components/schemas/LoginRequest", login.at("/requestBody/content/application~1json/schema/$ref").asText());
        assertTrue(login.at("/requestBody/required").asBoolean());
        Set<String> codigos = new java.util.HashSet<>();
        login.get("responses").fieldNames().forEachRemaining(codigos::add);
        assertEquals(Set.of("200", "400", "401"), codigos);
        for (String codigo : codigos) {
            assertEquals("#/components/schemas/" + (codigo.equals("200") ? "TokenResponse" : "ApiError"),
                    login.get("responses").get(codigo).at("/content/application~1json/schema/$ref").asText());
        }
        assertTrue(login.at("/responses/401/description").asText().contains("filtros"));
        assertFalse(documento.get("components").get("securitySchemes").has("basicAuth"));
        var schemas = documento.at("/components/schemas");
        var senha = schemas.at("/LoginRequest/properties/senha");
        assertTrue(senha.get("writeOnly").asBoolean());
        assertEquals("password", senha.get("format").asText());
        assertTrue(senha.get("description").asText().contains("72 bytes UTF-8"));
        assertFalse(senha.has("maxLength"));
        assertFalse(senha.has("example"));
        Set<String> campos = new java.util.HashSet<>();
        schemas.at("/TokenResponse/properties").fieldNames().forEachRemaining(campos::add);
        assertEquals(Set.of("accessToken", "tokenType", "expiresIn", "expiresAt"), campos);
        assertEquals("Bearer", schemas.at("/TokenResponse/properties/tokenType/example").asText());
        assertEquals(1800, schemas.at("/TokenResponse/properties/expiresIn/example").asInt());
        assertEquals("date-time", schemas.at("/TokenResponse/properties/expiresAt/format").asText());
        assertFalse(schemas.at("/TokenResponse/properties/accessToken").has("example"));
        assertFalse(schemas.get("TokenResponse").toString().contains("senha"));
        assertFalse(schemas.toString().contains("senhaHash"));
        assertFalse(schemas.toString().contains("JWT_SECRET"));
        for (String rota : List.of("/api/contas-acesso/me", "/api/contas-acesso/me/senha")) {
            var operacao = documento.get("paths").get(rota).elements().next();
            assertTrue(operacao.get("description").asText().contains("Bearer JWT"));
            assertFalse(operacao.get("description").asText().contains("HTTP Basic"));
            assertFalse(operacao.at("/responses/401").has("content"));
        }
    }

    @Test
    void profileProdDeveManterOpenApiESwaggerDesabilitados() throws Exception {
        var propriedades = new java.util.Properties();
        try (var arquivo = Files.newInputStream(Path.of("src/main/resources/application-prod.properties"))) {
            propriedades.load(arquivo);
        }
        assertEquals("false", propriedades.getProperty("springdoc.api-docs.enabled"));
        assertEquals("false", propriedades.getProperty("springdoc.swagger-ui.enabled"));
    }
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class ContextoSemBanco {
        @org.springframework.context.annotation.Bean
        org.springframework.security.provisioning.InMemoryUserDetailsManager usuarioFicticio(
                org.springframework.security.crypto.password.PasswordEncoder encoder) {
            return new org.springframework.security.provisioning.InMemoryUserDetailsManager(
                    org.springframework.security.core.userdetails.User.withUsername("documentacao.ficticia")
                            .password(encoder.encode("senha ficticia documentacao 2026")).roles("OPERADOR").build());
        }
    }
}
