
package com.curso.instrumento2026.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import java.math.BigDecimal;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer politicaDeAutorizacaoHttp() {
        return openApi -> openApi.getPaths().forEach((caminho, item) ->
                item.readOperationsMap().forEach((metodo, operacao) -> {
                    if (metodo == PathItem.HttpMethod.POST && Set.of("/api/autenticacao/login",
                            "/api/autenticacao/refresh", "/api/autenticacao/logout", "/api/autenticacao/logout-todos").contains(caminho)) {
                        documentarSessao(caminho, operacao);
                        return;
                    }
                    boolean publica = (metodo == PathItem.HttpMethod.GET && caminho.equals("/api/health"))
                            || (metodo == PathItem.HttpMethod.POST && caminho.equals("/api/autenticacao/login"));
                    if (publica) return;
                    boolean administrativa = metodo == PathItem.HttpMethod.DELETE
                            || (metodo == PathItem.HttpMethod.POST && caminho.equals("/api/contas-acesso"))
                            || caminho.matches("/api/contas-acesso/[^/]+/(perfis|status)");
                    String permissao = administrativa ? "ADMINISTRADOR" : "OPERADOR ou ADMINISTRADOR";
                    String descricao = operacao.getDescription();
                    operacao.setDescription((descricao == null || descricao.isBlank() ? "" : descricao + "\n\n")
                            + "Autorização: exige Bearer JWT com perfil " + permissao + ".");
                    operacao.getResponses().putIfAbsent("401", new ApiResponse().description(
                            "Bearer ausente, inválido ou expirado. Resposta dos filtros de segurança, sem contrato JSON ApiError."));
                    operacao.getResponses().putIfAbsent("403", new ApiResponse().description(
                            "Token autenticado sem perfil suficiente. Resposta dos filtros de segurança, sem contrato JSON ApiError."));
                }));
    }

    private static void documentarSessao(String caminho, io.swagger.v3.oas.models.Operation operacao) {
        boolean login = caminho.endsWith("/login");
        boolean refresh = caminho.endsWith("/refresh");
        boolean global = caminho.endsWith("/logout-todos");
        operacao.setTags(java.util.List.of("Autenticação e sessões"));
        operacao.setSecurity(global ? java.util.List.of(new SecurityRequirement().addList("bearerAuth")) : java.util.List.of());
        operacao.setSummary(login ? "Autenticar e iniciar sessão" : refresh ? "Renovar e rotacionar tokens"
                : global ? "Encerrar sessões da própria conta" : "Encerrar família de sessão");
        String descricao = login
                ? "Operação pública. Conta e pessoa ativas; retorna access JWT HS256 (padrão: 30 minutos) e refresh opaco."
                : refresh ? "Operação pública. Cada renovação substitui o par e utiliza a geração anterior. Preserva sid e limite absoluto; replay revoga toda a família."
                : global ? "Exige Bearer JWT válido, sem requisito adicional de perfil. Usa somente o subject autenticado; revalida conta e pessoa e revoga suas famílias. Não aceita seleção de outra conta."
                : "Operação pública. Encerra a família identificada pelo refresh, inclusive uma geração anterior. Token desconhecido, ausente ou já revogado recebe o mesmo 204, sem enumeração.";
        operacao.setDescription(descricao + " A sessão de refresh é persistida, sem HttpSession. "
                + "Logout não invalida imediatamente o access token: a validação continua local e stateless até expirar. "
                + "Refresh padrão: inatividade PT30M e duração absoluta PT8H, configuráveis. Nunca registre tokens ou hashes. "
                + "Bearer inválido apresentado mesmo em rota pública pode ser rejeitado pelos filtros antes do controller.");
        var respostas = new io.swagger.v3.oas.models.responses.ApiResponses();
        respostas.addApiResponse(login || refresh ? "200" : "204", login || refresh
                ? respostaJson("Par de tokens; substituir access e refresh juntos no cliente", "TokenResponse")
                : new ApiResponse().description("Sessões encerradas, sem corpo"));
        if (!global) respostas.addApiResponse("400", respostaJson(login
                ? "JSON malformado ou campos de login inválidos" : "JSON malformado", "ApiError"));
        if (login || refresh || global) respostas.addApiResponse("401", respostaJson(
                (refresh ? "Refresh token inválido ou expirado: ausente, vazio, desconhecido, utilizado, revogado, expirado ou conta/pessoa inativa. Mensagem genérica em ApiError. "
                        : login ? "Credenciais inválidas em ApiError. " : "Identidade inexistente ou conta/pessoa inativa em ApiError. ")
                + "Bearer ausente (na operação protegida), inválido ou expirado é rejeitado pelos filtros de segurança, com WWW-Authenticate e sem corpo ApiError. O conteúdo JSON descrito aplica-se somente à rejeição do controller.", "ApiError"));
        // Logout público também pode rejeitar um Bearer inválido apresentado pelo cliente.
        if (!login && !refresh && !global) respostas.addApiResponse("401", new ApiResponse().description(
                "Somente se um Bearer inválido/expirado for apresentado: filtros de segurança, sem corpo ApiError"));
        operacao.setResponses(respostas);
    }

    private static ApiResponse respostaJson(String descricao, String schema) {
        return new ApiResponse().description(descricao).content(new io.swagger.v3.oas.models.media.Content()
                .addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/" + schema))));
    }

    @Bean
    public OpenApiCustomizer schemasDoCicloDeTokens() {
        return api -> {
            var schemas = api.getComponents().getSchemas();
            var resposta = schemas.get("TokenResponse");
            resposta.setDescription("Par de credenciais sensíveis; não registrar em logs. expiresAt é o limite do access; refreshTokenExpiresAt é o limite absoluto da família, não o timeout por inatividade.");
            resposta.setRequired(java.util.List.of("accessToken", "tokenType", "expiresIn", "expiresAt", "refreshToken", "refreshTokenExpiresAt"));
            ((io.swagger.v3.oas.models.media.Schema<?>) resposta.getProperties().get("accessToken")).setReadOnly(true);
            var refresh = (io.swagger.v3.oas.models.media.Schema<?>) resposta.getProperties().get("refreshToken");
            refresh.setDescription("Credencial opaca de uso único, 32 bytes aleatórios em Base64 URL sem padding; nunca enviar à API de negócio");
            refresh.setReadOnly(true);
            var limite = (io.swagger.v3.oas.models.media.Schema<?>) resposta.getProperties().get("refreshTokenExpiresAt");
            limite.setDescription("Limite absoluto UTC da família, preservado nas rotações; a inatividade pode encerrar antes");
            limite.setFormat("date-time");
            var request = schemas.get("RefreshTokenRequest");
            request.setDescription("Refresh opaco. Ausente/vazio: refresh responde 401 genérico; logout responde 204. Nunca registrar a credencial.");
            var segredo = (io.swagger.v3.oas.models.media.Schema<?>) request.getProperties().get("refreshToken");
            segredo.setWriteOnly(true);
            segredo.setDescription("Refresh token emitido no par anterior; não contém dados pessoais");
        };
    }

    @Bean
    public OpenApiCustomizer quantidadeInteiraDoItemVenda() {
        return openApi -> {
            var item = openApi.getComponents().getSchemas().get("ItemVendaRequest");
            if (item == null) return;
            var quantidade = (io.swagger.v3.oas.models.media.Schema<?>) item.getProperties().get("quantidade");
            // A geração automática reaplica @DecimalMin/@Digits; o domínio exige unidades inteiras.
            quantidade.setType("integer");
            quantidade.setTypes(Set.of("integer"));
            quantidade.setFormat("int32");
            quantidade.setMinimum(BigDecimal.ONE);
            quantidade.setMaximum(BigDecimal.valueOf(Integer.MAX_VALUE));
            quantidade.setMultipleOf(BigDecimal.ONE);
        };
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(new Info()
                        .title("API de Controle de Instrumentos Musicais")
                        .description("API para gerenciamento de instrumentos musicais, categorias, fornecedores, estoque, pessoas, clientes, colaboradores e vendas.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Curso de Spring Boot 2026")))
                .externalDocs(new ExternalDocumentation()
                        .description("Material didático do projeto")
                        .url("https://github.com/jeffersonarpasserini/suporteos2026"));
    }
}
