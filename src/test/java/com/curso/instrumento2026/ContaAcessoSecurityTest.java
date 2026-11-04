package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.Pessoa;
import com.curso.instrumento2026.domain.StatusContaAcesso;
import com.curso.instrumento2026.repository.PessoaRepository;
import com.curso.instrumento2026.service.ContaAcessoService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNull;
import static com.curso.instrumento2026.support.JwtTestSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Sem transação de teste: a autenticação consulta contas efetivamente confirmadas no PostgreSQL.
@SpringBootTest(properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContaAcessoSecurityTest {
    private static final String SENHA = "  Frase Secreta Security 2026  ";
    @Autowired MockMvc mvc;
    @Autowired PessoaRepository pessoas;
    @Autowired ContaAcessoService contas;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private Long pessoaId;
    private Long contaId;
    private String login;
    private String emailBootstrap;

    @BeforeEach
    void preparar() {
        String execucao = UUID.randomUUID().toString();
        login = "security." + execucao;
        emailBootstrap = execucao + ".bootstrap@example.com";
        pessoaId = new TransactionTemplate(transactionManager).execute(status -> pessoas.save(
                new Pessoa("Pessoa security", execucao + "@example.com", "52998224725", LocalDate.now())).getId());
        contaId = contas.cadastrar(pessoaId, login, SENHA).getId();
    }

    @AfterEach
    void limpar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM conta_acesso WHERE pessoa_id=?", pessoaId);
            jdbc.update("DELETE FROM pessoa WHERE id=? OR email=?", pessoaId, emailBootstrap);
        });
    }

    @Test
    void healthDeveSerPublicoParaGetEHead() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(content().string("OK"));
        mvc.perform(head("/api/health")).andExpect(status().isOk());
        mvc.perform(post("/api/health")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/pessoas", "/api/clientes", "/api/colaboradores", "/api/categorias",
            "/api/fornecedores", "/api/instrumentos", "/api/vendas"})
    void consultasComerciaisAnonimasDevemSerNegadas(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")));
    }

    @Test
    void bootstrapPessoaDeveSerPublicoSemCsrfComDemaisEscritasProtegidas() throws Exception {
        String corpo = """
                {"nome":"Pessoa bootstrap security","email":"%s","cpf":"11144477735"}
                """.formatted(emailBootstrap);
        mvc.perform(post("/api/pessoas").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("email").value(emailBootstrap));
        mvc.perform(put("/api/pessoas/{id}", pessoaId).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/instrumentos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroPublicoDeveValidarCamposSemLiberarOutrosMetodos() throws Exception {
        mvc.perform(post("/api/contas-acesso").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("status").value(400))
                .andExpect(jsonPath("fields.pessoaId").exists())
                .andExpect(jsonPath("fields.login").exists())
                .andExpect(jsonPath("fields.senha").exists());
        for (var metodo : new org.springframework.http.HttpMethod[]{
                org.springframework.http.HttpMethod.GET, org.springframework.http.HttpMethod.HEAD,
                org.springframework.http.HttpMethod.PUT, org.springframework.http.HttpMethod.PATCH,
                org.springframework.http.HttpMethod.DELETE, org.springframework.http.HttpMethod.OPTIONS}) {
            mvc.perform(request(metodo, "/api/contas-acesso")).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/contas-acesso/me")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/contas-acesso/me/senha").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void contaPersistidaDeveAutenticarComLoginNormalizadoESenhaIntegral() throws Exception {
        mvc.perform(get("/api/pessoas/{id}", pessoaId)
                        .with(bearer(mvc, "  " + login.toUpperCase(Locale.ROOT) + "  ", SENHA)))
                .andExpect(status().isOk()).andExpect(jsonPath("id").value(pessoaId));
        // O usuário OPERADOR também pode escrever: não há restrição administrativa nesta aula.
        mvc.perform(put("/api/pessoas/{id}/status", pessoaId).with(bearer(mvc, login, SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ATIVO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void senhaIncorretaOuNormalizadaDeveSerRejeitada() throws Exception {
        mvc.perform(loginRequest(login, "Senha incorreta 2026"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Login ou senha inválidos"))
                .andExpect(jsonPath("accessToken").doesNotExist());
        mvc.perform(loginRequest(login, SENHA.trim())).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Login ou senha inválidos"));
    }

    @Test
    void bloqueioDeveImpedirNovoLoginSemRevogarTokenEmitido() throws Exception {
        String emitido = token(mvc, login, SENHA);
        mvc.perform(get("/api/pessoas/{id}", pessoaId).with(bearer(emitido)))
                .andExpect(status().isOk());
        contas.alterarStatus(contaId, StatusContaAcesso.BLOQUEADA);
        mvc.perform(loginRequest(login, SENHA)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Login ou senha inválidos"));
        mvc.perform(get("/api/pessoas/{id}", pessoaId).with(bearer(emitido))).andExpect(status().isOk());
        contas.alterarStatus(contaId, StatusContaAcesso.ATIVA);
        mvc.perform(get("/api/pessoas/{id}", pessoaId).with(bearer(mvc, login, SENHA)))
                .andExpect(status().isOk());
    }

    @Test
    void pessoaInativaDeveImpedirLoginMesmoComContaAtiva() throws Exception {
        String emitido = token(mvc, login, SENHA);
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                pessoas.findById(pessoaId).orElseThrow().inativar());
        mvc.perform(loginRequest(login, SENHA)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Login ou senha inválidos"));
        mvc.perform(get("/api/pessoas/{id}", pessoaId).with(bearer(emitido))).andExpect(status().isOk());
    }

    @Test
    void contaInexistenteDeveSerRejeitada() throws Exception {
        mvc.perform(loginRequest("inexistente." + login, SENHA))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("message").value("Login ou senha inválidos"));
    }

    @Test
    void autenticacaoNaoDeveCriarSessaoNemPersistirEntreRequisicoes() throws Exception {
        var autenticada = mvc.perform(get("/api/pessoas/{id}", pessoaId).with(bearer(mvc, login, SENHA)))
                .andExpect(status().isOk()).andExpect(cookie().doesNotExist("JSESSIONID")).andReturn();
        assertNull(autenticada.getRequest().getSession(false));
        var anonima = mvc.perform(get("/api/pessoas/{id}", pessoaId))
                .andExpect(status().isUnauthorized()).andExpect(cookie().doesNotExist("JSESSIONID")).andReturn();
        assertNull(anonima.getRequest().getSession(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/swagger-config",
            "/swagger-ui/index.html"})
    void documentacaoDeveSerPublicaQuandoHabilitada(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isOk());
        mvc.perform(head(rota)).andExpect(status().isOk());
        mvc.perform(post(rota)).andExpect(status().isUnauthorized());
    }

    @Test
    void entradaSwaggerDeveRedirecionarPublicamente() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/swagger-ui/index.html")));
    }
}
