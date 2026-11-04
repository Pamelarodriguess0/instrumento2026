package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.Pessoa;
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
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.curso.instrumento2026.support.JwtTestSupport.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
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
    private Long pessoaId;
    private String login;

    @BeforeEach
    void preparar() {
        String token = UUID.randomUUID().toString();
        login = "jwt." + token;
        pessoaId = new TransactionTemplate(transactionManager).execute(status -> pessoas.save(new Pessoa(
                "Pessoa JWT", token + "@example.com", "52998224725", LocalDate.now())).getId());
        contas.cadastrar(pessoaId, login, SENHA);
    }

    @AfterEach
    void limpar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM conta_acesso WHERE pessoa_id=?", pessoaId);
            jdbc.update("DELETE FROM pessoa WHERE id=?", pessoaId);
        });
    }

    @Test
    void loginDeveRetornarBearerComClaimsControladosPeloServidorEIdentificarMe() throws Exception {
        var resultado = mvc.perform(loginRequest(login, SENHA)).andExpect(status().isOk())
                .andExpect(jsonPath("tokenType").value("Bearer")).andExpect(jsonPath("expiresIn").value(1800))
                .andExpect(jsonPath("$.length()").value(4)).andExpect(jsonPath("senha").doesNotExist())
                .andExpect(jsonPath("senhaHash").doesNotExist()).andReturn();
        String emitido = JsonPath.read(resultado.getResponse().getContentAsString(), "$.accessToken");
        String expiresAt = JsonPath.read(resultado.getResponse().getContentAsString(), "$.expiresAt");
        var jwt = decoder.decode(emitido);
        assertEquals(Set.of("iss", "sub", "iat", "exp", "jti", "perfis"), jwt.getClaims().keySet());
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
        return encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder().issuer(issuer).subject(login)
                .issuedAt(iat).expiresAt(exp).id(UUID.randomUUID().toString()).claim("perfis", List.of("OPERADOR"))
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
