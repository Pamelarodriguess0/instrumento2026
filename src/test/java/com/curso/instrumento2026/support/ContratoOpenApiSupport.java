package com.curso.instrumento2026.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Allows only the explicitly tested evolution while comparing the complete historical contract. */
public final class ContratoOpenApiSupport {
    private ContratoOpenApiSupport() { }

    public static void verificarCompatibilidadeComAula10(JsonNode documento) throws Exception {
        var publicado = documento.deepCopy();
        var exportado = Yaml.mapper().readTree(Files.readString(Path.of("docs/aula10/openapi.yaml")));
        assertEquals(Yaml.mapper().readTree("[{\"bearerAuth\":[]}]"), publicado.get("security"));
        var esquemas = ((ObjectNode) publicado.get("components")).remove("securitySchemes");
        assertEquals(Yaml.mapper().readTree("{\"bearerAuth\":{\"type\":\"http\",\"scheme\":\"bearer\",\"bearerFormat\":\"JWT\"}}"), esquemas);
        var health = (ObjectNode) publicado.at("/paths/~1api~1health/get");
        assertEquals(Yaml.mapper().readTree("[]"), health.remove("security"));

        // Aula 14 adds only a fixed permission suffix and filter responses to historical operations.
        exportado.get("paths").fields().forEachRemaining(rota -> rota.getValue().fields().forEachRemaining(metodo -> {
            if (rota.getKey().equals("/api/health") && metodo.getKey().equals("get")) return;
            var operacao = (ObjectNode) publicado.get("paths").get(rota.getKey()).get(metodo.getKey());
            assertFalse(operacao.has("security"), "Protected historical operations inherit Bearer: " + rota.getKey());
            String regra = "Autorização: exige Bearer JWT com perfil "
                    + (metodo.getKey().equals("delete") ? "ADMINISTRADOR" : "OPERADOR ou ADMINISTRADOR") + ".";
            String descricao = operacao.path("description").asText();
            assertTrue(descricao.endsWith(regra), rota.getKey());
            if (descricao.equals(regra)) operacao.remove("description");
            else {
                assertTrue(descricao.endsWith("\n\n" + regra), rota.getKey());
                operacao.put("description", descricao.substring(0, descricao.length() - regra.length() - 2));
            }
            var respostas = (ObjectNode) operacao.get("responses");
            for (String codigo : List.of("401", "403")) {
                assertFalse(metodo.getValue().get("responses").has(codigo));
                var seguranca = respostas.remove(codigo);
                assertNotNull(seguranca, rota.getKey() + " " + codigo);
                assertFalse(seguranca.has("content"));
                assertTrue(seguranca.path("description").asText().contains("filtros de segurança"));
            }
        }));

        // Preserve and validate the exact concurrence descriptions introduced in Aula 11.
        for (String rota : List.of("/api/vendas/{id}/finalizacao", "/api/instrumentos/{id}/estoque/entradas",
                "/api/instrumentos/{id}/estoque/saidas")) {
            var operacao = (ObjectNode) publicado.get("paths").get(rota).get("post");
            String descricao = operacao.get("description").asText();
            assertTrue(descricao.contains("bloqueio pessimista"));
            assertTrue(descricao.contains("transação"));
            if (rota.equals("/api/vendas/{id}/finalizacao")) {
                assertTrue(descricao.contains("ordem crescente de ID"));
                assertTrue(descricao.contains("evitando que duas vendas consumam o mesmo estoque"));
            } else {
                assertTrue(descricao.contains("quantidade inteira positiva"));
                assertTrue(descricao.contains("Repetir a chamada repete o efeito"));
                if (rota.endsWith("/saidas")) assertTrue(descricao.contains("sem permitir saldo negativo"));
            }
            operacao.put("description", exportado.get("paths").get(rota).get("post").get("description").asText());
        }
        var caminhos = (ObjectNode) publicado.get("paths");
        for (var novo : Map.of("/api/contas-acesso", "post", "/api/contas-acesso/me", "get",
                "/api/contas-acesso/me/senha", "put", "/api/autenticacao/login", "post",
                "/api/contas-acesso/{id}/perfis", "put", "/api/contas-acesso/{id}/status", "put").entrySet()) {
            assertFalse(exportado.get("paths").has(novo.getKey()));
            var caminho = caminhos.remove(novo.getKey());
            assertNotNull(caminho, novo.getKey());
            assertEquals(1, caminho.size(), novo.getKey());
            assertTrue(caminho.has(novo.getValue()), novo.getKey());
        }
        var schemas = (ObjectNode) publicado.get("components").get("schemas");
        for (String schema : List.of("ContaAcessoRequest", "ContaAcessoResponse", "AlterarSenhaContaRequest",
                "LoginRequest", "TokenResponse", "PerfisContaAcessoRequest", "StatusContaAcessoRequest")) {
            assertFalse(exportado.get("components").get("schemas").has(schema));
            assertNotNull(schemas.remove(schema), schema);
        }
        assertEquals(exportado.get("paths"), caminhos);
        assertEquals(exportado.get("components"), publicado.get("components"));
        assertEquals(exportado.get("info"), publicado.get("info"));
    }
}
