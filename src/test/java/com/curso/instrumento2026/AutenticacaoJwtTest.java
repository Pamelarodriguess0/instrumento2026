package com.curso.instrumento2026;

import static com.curso.instrumento2026.support.AdminFixtureSupport.comoAdministrador;

import com.curso.instrumento2026.domain.Pessoa;
import com.curso.instrumento2026.domain.PerfilAcesso;
import com.curso.instrumento2026.domain.StatusContaAcesso;
import com.curso.instrumento2026.repository.PessoaRepository;
import com.curso.instrumento2026.service.ContaAcessoService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.curso.instrumento2026.support.JwtTestSupport.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutenticacaoJwtTest {
    private static final String SENHA = "  Frase de Teste JWT 2026  ";
    private static final String ISSUER = "https://instrumento2026.local";
    @Autowired MockMvc mvc;
    @Autowired PessoaRepository pessoas;
    @Autowired ContaAcessoService contas;
    @Autowired JwtDecoder decoder;
    @Autowired JwtEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoSpyBean AuthenticationManager authenticationManager;
    private Long pessoaId;
    private Long contaId;
    private String login;

    @BeforeEach
    void preparar() {
        String token = UUID.randomUUID().toString();
        login = "jwt." + token;
        pessoaId = new TransactionTemplate(transactionManager).execute(status -> pessoas.save(new Pessoa(
                "Pessoa JWT", token + "@example.com", "52998224725", LocalDate.now())).getId());
        contaId = comoAdministrador(() -> contas.cadastrar(pessoaId, login, SENHA)).getId();
    }

    @AfterEach
    void limpar() throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            limparAuditoriaDaFixture(jdbc, contaId, login, login + ".inexistente");
            jdbc.update("DELETE FROM conta_acesso WHERE pessoa_id=?", pessoaId);
            jdbc.update("DELETE FROM pessoa WHERE id=?", pessoaId);
        });
    }

    @Test
    void falhasConcorrentesDevemAtivarBloqueioUmaVezERecusarSenhaCorreta() throws Exception {
        String loginAlternativo = "  " + login.toUpperCase(Locale.ROOT) + "  ";
        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            verificarLoginRecusado(loginAlternativo, "senha-incorreta");
            var estado = estadoProtecao(login);
            assertEquals(tentativa, estado.get("falhas_consecutivas"));
            assertNull(estado.get("bloqueada_ate"));
        }
        var inicio = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Void> falha = () -> {
                inicio.await(5, TimeUnit.SECONDS);
                verificarLoginRecusado(loginAlternativo, "senha-incorreta");
                return null;
            };
            var primeira = executor.submit(falha);
            var segunda = executor.submit(falha);
            primeira.get(15, TimeUnit.SECONDS);
            segunda.get(15, TimeUnit.SECONDS);
        }
        var estadoBloqueado = estadoProtecao(login);
        assertEquals(5, estadoBloqueado.get("falhas_consecutivas"));
        assertNotNull(estadoBloqueado.get("bloqueada_ate"));
        clearInvocations(authenticationManager);
        verificarLoginRecusado(login, SENHA);
        verifyNoInteractions(authenticationManager);
        assertEquals(estadoBloqueado, estadoProtecao(login));
        assertEquals(List.of("LOGIN_FALHA", "LOGIN_FALHA", "LOGIN_FALHA", "LOGIN_FALHA", "LOGIN_FALHA",
                        "BLOQUEIO_TEMPORARIO_ATIVADO", "LOGIN_RECUSADO_BLOQUEIO"),
                jdbc.queryForList("SELECT tipo FROM evento_seguranca WHERE identificador_hash = ? ORDER BY id",
                        String.class, chaveLogin(login)));

        String inexistente = login + ".inexistente";
        verificarLoginRecusado(inexistente, SENHA);
        assertEquals(1, estadoProtecao(inexistente).get("falhas_consecutivas"));
        var evento = jdbc.queryForMap("SELECT tipo, ator_conta_id, alvo_conta_id, endereco_origem "
                + "FROM evento_seguranca WHERE identificador_hash = ?", chaveLogin(inexistente));
        assertEquals("LOGIN_FALHA", evento.get("tipo"));
        assertEquals("192.0.2.10", evento.get("endereco_origem"));
        assertNull(evento.get("ator_conta_id"));
        assertNull(evento.get("alvo_conta_id"));
    }

    @Test
    void sucessoDeveLimparFalhasEEmitirParDeTokens() throws Exception {
        verificarLoginRecusado(login, "senha-incorreta");
        assertEquals(1, estadoProtecao(login).get("falhas_consecutivas"));

        mvc.perform(loginRequest(login, SENHA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        var estado = estadoProtecao(login);
        assertEquals(0, estado.get("falhas_consecutivas"));
        assertNull(estado.get("janela_iniciada_em"));
        assertNull(estado.get("bloqueada_ate"));
    }

    @Test
    void deveRecusarLoginSeBloqueioAdministrativoConfirmarAntesDaEmissao() throws Exception {
        var bloqueio = new TransactionTemplate(transactionManager);
        bloqueio.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        bloqueio.setTimeout(5);

        doAnswer(invocation -> {
            var authentication = invocation.callRealMethod();
            bloqueio.executeWithoutResult(status ->
                    comoAdministrador(() -> contas.alterarStatus(contaId, StatusContaAcesso.BLOQUEADA)));
            return authentication;
        }).when(authenticationManager).authenticate(any());

        verificarLoginRecusado(login, SENHA);
        assertEquals("BLOQUEADA",
                jdbc.queryForObject("SELECT status FROM conta_acesso WHERE id = ?", String.class, contaId));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT count(*) FROM sessao_refresh_token WHERE conta_acesso_id = ?", Long.class, contaId));
    }

    private void verificarLoginRecusado(String identificador, String senha) throws Exception {
        mvc.perform(loginRequest(identificador, senha).header("X-Forwarded-For", "198.51.100.20")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.10");
                            return request;
                        }))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Login ou senha inválidos"))
                .andExpect(jsonPath("$.path").value("/api/autenticacao/login"))
                .andExpect(jsonPath("$.fields").isEmpty());
    }

    private Map<String, Object> estadoProtecao(String identificador) throws Exception {
        return jdbc.queryForMap("SELECT falhas_consecutivas, janela_iniciada_em, bloqueada_ate "
                + "FROM protecao_autenticacao WHERE chave_login = ?", chaveLogin(identificador));
    }

    @Test
    void auditoriaDeveExigirAdministradorEPaginarEventosDoTipoSolicitado() throws Exception {
        verificarLoginRecusado(login, "senha-incorreta");
        verificarLoginRecusado(login, "outra-senha-incorreta");
        String caminho = "/api/auditoria-seguranca";
        mvc.perform(get(caminho)).andExpect(status().isUnauthorized());
        mvc.perform(get(caminho).with(bearer(token(mvc, login, SENHA)))).andExpect(status().isForbidden());

        Instant agora = Instant.now();
        String administrador = assinar(ISSUER, agora.minusSeconds(5), agora.plusSeconds(300), "ADMINISTRADOR");
        var ids = jdbc.queryForList("SELECT id FROM evento_seguranca WHERE tipo = 'LOGIN_FALHA' "
                + "AND identificador_hash = ? ORDER BY ocorrido_em DESC, id DESC", Long.class, chaveLogin(login));
        assertEquals(2, ids.size());
        Long eventoAlheio = jdbc.queryForObject("INSERT INTO evento_seguranca (tipo, ocorrido_em, identificador_hash) "
                        + "VALUES ('LOGIN_FALHA', clock_timestamp(), ?) RETURNING id",
                Long.class, chaveLogin(login + ".alheio"));
        try {
            var encontrados = new ArrayList<Long>();
            int totalPaginas;
            for (int pagina = 0; ; pagina++) {
                var resultado = mvc.perform(get(caminho).param("tipo", "LOGIN_FALHA")
                                .param("page", String.valueOf(pagina)).param("size", "1").with(bearer(administrador)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.number").value(pagina))
                        .andReturn();
                var json = JsonMapper.builder().build().readTree(resultado.getResponse().getContentAsString());
                var conteudo = json.path("content");
                assertTrue(conteudo.size() <= 1);
                for (var evento : conteudo) {
                    long id = evento.path("id").asLong();
                    if (ids.contains(id)) {
                        encontrados.add(id);
                        assertEquals("LOGIN_FALHA", evento.path("tipo").asText());
                        assertFalse(evento.has("identificadorHash"));
                    }
                }
                totalPaginas = json.path("page").path("totalPages").asInt();
                if (encontrados.size() == ids.size() || pagina + 1 >= totalPaginas) break;
            }
            assertEquals(ids, encontrados);
        } finally {
            jdbc.update("DELETE FROM evento_seguranca WHERE id = ?", eventoAlheio);
        }
    }

    @Test
    void deveAuditarAdministracaoNaMesmaTransacao() {
        var transacao = new TransactionTemplate(transactionManager);
        transacao.executeWithoutResult(status -> {
            comoAdministrador(() -> contas.alterarStatus(contaId, StatusContaAcesso.BLOQUEADA));
            assertEquals(List.of("CONTA_CRIADA", "STATUS_ALTERADO"), eventosDaConta());
            status.setRollbackOnly();
        });
        assertEquals(List.of("CONTA_CRIADA"), eventosDaConta());
        assertEquals("ATIVA",
                jdbc.queryForObject("SELECT status FROM conta_acesso WHERE id = ?", String.class, contaId));

        contas.alterarSenha(login, SENHA, "nova-senha-sintetica");
        comoAdministrador(() -> contas.alterarPerfis(contaId, Set.of(PerfilAcesso.OPERADOR)));
        comoAdministrador(() -> contas.alterarStatus(contaId, StatusContaAcesso.BLOQUEADA));
        assertEquals(List.of("CONTA_CRIADA", "SENHA_ALTERADA", "PERFIS_ALTERADOS", "STATUS_ALTERADO"), eventosDaConta());
    }

    @Test
    void deveAuditarRefreshAceitoRecusadoELogout() throws Exception {
        var loginInicial = mvc.perform(loginRequest(login, SENHA)).andExpect(status().isOk()).andReturn();
        String acesso = JsonPath.read(loginInicial.getResponse().getContentAsString(), "$.accessToken");
        String inicial = JsonPath.read(loginInicial.getResponse().getContentAsString(), "$.refreshToken");
        String corpo = "{\"refreshToken\":\"" + inicial + "\"}";
        var resultado = mvc.perform(post("/api/autenticacao/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andExpect(status().isOk()).andReturn();
        String renovado = JsonPath.read(resultado.getResponse().getContentAsString(), "$.refreshToken");
        mvc.perform(post("/api/autenticacao/refresh").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/autenticacao/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + renovado + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/autenticacao/logout-todos").with(bearer(token(mvc, login, SENHA))))
                .andExpect(status().isNoContent());
        assertEquals(List.of("CONTA_CRIADA", "LOGIN_SUCESSO", "REFRESH_SUCESSO", "REFRESH_FALHA",
                "LOGOUT", "LOGIN_SUCESSO", "LOGOUT_GLOBAL"), eventosDaConta());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM evento_seguranca WHERE alvo_conta_id = ? "
                + "AND tipo IN ('REFRESH_SUCESSO', 'REFRESH_FALHA', 'LOGOUT', 'LOGOUT_GLOBAL') "
                + "AND identificador_hash IS NOT NULL", Long.class, contaId));
        String hashSenha = jdbc.queryForObject("SELECT senha_hash FROM conta_acesso WHERE id = ?", String.class, contaId);
        var registros = jdbc.queryForList("SELECT row_to_json(e)::text FROM evento_seguranca e "
                + "WHERE alvo_conta_id = ? OR identificador_hash = ?", String.class, contaId, chaveLogin(login));
        for (String registro : registros) {
            assertTrue(List.of(login, SENHA, hashSenha, acesso, inicial, renovado).stream()
                    .noneMatch(registro::contains), "Evento contém dado de autenticação que deveria ser omitido");
        }
    }

    private List<String> eventosDaConta() {
        return jdbc.queryForList("SELECT tipo FROM evento_seguranca WHERE alvo_conta_id = ? ORDER BY id",
                String.class, contaId);
    }

    @Test
    void loginDeveRetornarBearerComClaimsControladosPeloServidorEIdentificarMe() throws Exception {
        var resultado = mvc.perform(loginRequest(login, SENHA)).andExpect(status().isOk())
                .andExpect(jsonPath("tokenType").value("Bearer")).andExpect(jsonPath("expiresIn").value(1800))
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("refreshToken").isNotEmpty())
                .andExpect(jsonPath("refreshTokenExpiresAt").isString())
                .andExpect(jsonPath("senha").doesNotExist())
                .andExpect(jsonPath("senhaHash").doesNotExist()).andReturn();
        String emitido = JsonPath.read(resultado.getResponse().getContentAsString(), "$.accessToken");
        String expiresAt = JsonPath.read(resultado.getResponse().getContentAsString(), "$.expiresAt");
        var jwt = decoder.decode(emitido);
        Map<String, Object> resposta = JsonPath.read(resultado.getResponse().getContentAsString(), "$");
        assertEquals(Set.of("accessToken", "tokenType", "expiresIn", "expiresAt", "refreshToken", "refreshTokenExpiresAt"),
                resposta.keySet());
        assertEquals(Set.of("iss", "sub", "iat", "exp", "jti", "perfis", "sid"), jwt.getClaims().keySet());
        String sid = jwt.getClaimAsString("sid");
        assertNotNull(sid);
        assertNotNull(UUID.fromString(sid));
        assertEquals(jdbc.queryForObject("SELECT s.familia_id FROM sessao_refresh_token s "
                + "JOIN conta_acesso c ON c.id=s.conta_acesso_id WHERE c.pessoa_id=?", String.class, pessoaId), sid);
        assertTrue(Instant.parse((String) resposta.get("refreshTokenExpiresAt")).isAfter(jwt.getIssuedAt()));
        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertEquals(ISSUER, jwt.getIssuer().toString());
        assertEquals(login, jwt.getSubject());
        assertNotNull(UUID.fromString(jwt.getId()));
        assertEquals(List.of("OPERADOR"), jwt.getClaimAsStringList("perfis"));
        assertEquals(Duration.ofSeconds(1800), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
        assertEquals(Instant.parse(expiresAt), jwt.getExpiresAt());
        assertFalse(jwt.getClaims().toString().contains(SENHA));
        mvc.perform(get("/api/contas-acesso/me").with(bearer(emitido))).andExpect(status().isOk())
                .andExpect(jsonPath("pessoaId").value(pessoaId)).andExpect(jsonPath("login").value(login));
    }

    @Test
    void jtiDeveSerDiferenteEmCadaEmissao() throws Exception {
        var primeiro = decoder.decode(token(mvc, login, SENHA));
        var segundo = decoder.decode(token(mvc, login, SENHA));
        assertNotEquals(primeiro.getId(), segundo.getId());
    }

    @Test
    void perfisESubjectEnviadosNoJsonNaoDevemAlterarClaims() throws Exception {
        String body = JsonMapper.builder().build().writeValueAsString(Map.of("login", login, "senha", SENHA,
                "perfis", List.of("ADMINISTRADOR"), "sub", "outra.conta"));
        var resultado = mvc.perform(post("/api/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        var jwt = decoder.decode(JsonPath.read(resultado.getResponse().getContentAsString(), "$.accessToken"));
        assertEquals(login, jwt.getSubject());
        assertEquals(List.of("OPERADOR"), jwt.getClaimAsStringList("perfis"));
    }

    @Test
    void basicNaoDeveMaisAutenticarRecursoProtegido() throws Exception {
        mvc.perform(get("/api/contas-acesso/me").with(httpBasic(login, SENHA)))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", containsString("Bearer")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"token-malformado", "abc.def.ghi", ""})
    void bearerMalformadoDeveSerRejeitado(String valor) throws Exception {
        mvc.perform(get("/api/instrumentos").with(bearer(valor))).andExpect(status().isUnauthorized());
    }

    @Test
    void payloadAlteradoDeveSerRejeitado() throws Exception {
        String original = token(mvc, login, SENHA);
        String[] partes = original.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        String alterado = payload.replace(login, "outra.conta");
        assertNotEquals(payload, alterado);
        String adulterado = partes[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(alterado.getBytes(StandardCharsets.UTF_8)) + "." + partes[2];
        mvc.perform(get("/api/instrumentos").with(bearer(adulterado))).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoDeveRetornar401() throws Exception {
        Instant agora = Instant.now();
        String emitido = assinar(ISSUER, agora.minusSeconds(3600), agora.minusSeconds(180));
        mvc.perform(get("/api/instrumentos").with(bearer(emitido))).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeOutroEmissorDeveRetornar401() throws Exception {
        Instant agora = Instant.now();
        String emitido = assinar("https://outro.invalid", agora.minusSeconds(5), agora.plusSeconds(1800));
        mvc.perform(get("/api/instrumentos").with(bearer(emitido))).andExpect(status().isUnauthorized());
    }

    private String assinar(String issuer, Instant iat, Instant exp) {
        return assinar(issuer, iat, exp, "OPERADOR");
    }

    private String assinar(String issuer, Instant iat, Instant exp, String perfil) {
        return encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder().issuer(issuer).subject(login)
                .issuedAt(iat).expiresAt(exp).id(UUID.randomUUID().toString()).claim("perfis", List.of(perfil))
                .build())).getTokenValue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"login\":\"alguem\"}", "{\"login\":\"\",\"senha\":\"\"}", "{"})
    void loginDeveValidarEntradaSemRetornarToken(String json) throws Exception {
        mvc.perform(post("/api/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("status").value(400))
                .andExpect(jsonPath("accessToken").doesNotExist());
    }

    @Test
    void metodosDeLoginNaoDevemSerLiberadosIndiscriminadamente() throws Exception {
        mvc.perform(get("/api/autenticacao/login")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/autenticacao/login")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginNaoDeveCriarSessao() throws Exception {
        var resultado = mvc.perform(loginRequest(login, SENHA)).andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("JSESSIONID")).andReturn();
        assertNull(resultado.getRequest().getSession(false));
    }
}
