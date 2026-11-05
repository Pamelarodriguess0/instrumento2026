package com.curso.instrumento2026;

import static com.curso.instrumento2026.support.AdminFixtureSupport.comoAdministrador;

import com.curso.instrumento2026.domain.Pessoa;
import com.curso.instrumento2026.config.JwtProperties;
import org.springframework.security.oauth2.jwt.JwtEncoder;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static com.curso.instrumento2026.support.JwtTestSupport.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Filtros ativos, sem @Transactional: os requests usam contas e alterações confirmadas no PostgreSQL.
@SpringBootTest(properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContaAcessoApiTest {
    private static final String BASE = "/api/contas-acesso";
    private static final String SENHA = "  Frase secreta API 2026  ";
    private static final String NOVA = "Outra frase secreta API 2026";
    private final JsonMapper json = JsonMapper.builder().build();
    @Autowired MockMvc mvc;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtProperties jwtProperties;
    @Autowired PessoaRepository pessoas;
    @Autowired ContaAcessoService contas;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private Long pessoaId;
    private Long outraPessoaId;
    private Long contaId;
    private String login;
    private String outroLogin;

    @BeforeEach
    void preparar() {
        String execucao = UUID.randomUUID().toString();
        login = "api." + execucao;
        outroLogin = "outra." + execucao;
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            pessoaId = pessoas.save(new Pessoa("Pessoa API acesso", execucao + "@example.com",
                    "52998224725", LocalDate.now())).getId();
            outraPessoaId = pessoas.save(new Pessoa("Outra pessoa API", execucao + ".outra@example.com",
                    "11144477735", LocalDate.now())).getId();
        });
        contaId = comoAdministrador(() -> contas.cadastrar(pessoaId, login, SENHA)).getId();
    }

    @AfterEach
    void limpar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM conta_acesso WHERE pessoa_id IN (?,?)", pessoaId, outraPessoaId);
            jdbc.update("DELETE FROM pessoa WHERE id IN (?,?)", pessoaId, outraPessoaId);
        });
    }

    private String cadastro(Long pessoa, String usuario, String senha) {
        return json.writeValueAsString(Map.of("pessoaId", pessoa, "login", usuario, "senha", senha));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor administrador() {
        return bearerComPerfis(jwtEncoder, jwtProperties, "administrador.fixture", "ADMINISTRADOR");
    }

    private String troca(String atual, String nova) {
        return json.writeValueAsString(Map.of("senhaAtual", atual, "novaSenha", nova));
    }

    private void semSegredos(MvcResult resultado) throws Exception {
        String corpo = resultado.getResponse().getContentAsString();
        assertFalse(corpo.contains(SENHA));
        assertFalse(corpo.contains(NOVA));
        assertFalse(corpo.contains("senhaHash"));
        assertFalse(corpo.contains("senha_hash"));
        assertFalse(corpo.contains("{bcrypt}"));
        assertFalse(corpo.contains("$2a$"));
    }

    @Test
    void cadastroAnonimoDeveRetornar401SemCriarConta() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro(outraPessoaId, outroLogin, SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
                .andExpect(content().string(""));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM conta_acesso WHERE pessoa_id=?", Integer.class, outraPessoaId));
    }

    @Test
    void operadorNaoDeveCadastrarContaNemAlterarSeuPerfil() throws Exception {
        mvc.perform(post(BASE).with(bearer(mvc, login, SENHA)).contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro(outraPessoaId, outroLogin, SENHA)))
                .andExpect(status().isForbidden()).andExpect(content().string(""));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM conta_acesso WHERE pessoa_id=?", Integer.class, outraPessoaId));
        assertEquals(java.util.Set.of(com.curso.instrumento2026.domain.PerfilAcesso.OPERADOR),
                contas.buscarPorId(contaId).getPerfis());
    }

    @Test
    void cadastroAdministrativoDeveRetornar201ComOperadorAtivoESomenteCamposDeResposta() throws Exception {
        var resultado = mvc.perform(post(BASE).with(administrador()).contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro(outraPessoaId, "  " + outroLogin.toUpperCase(java.util.Locale.ROOT) + "  ", SENHA)))
                .andExpect(status().isCreated()).andExpect(jsonPath("pessoaId").value(outraPessoaId))
                .andExpect(jsonPath("login").value(outroLogin)).andExpect(jsonPath("nomePessoa").value("Outra pessoa API"))
                .andExpect(jsonPath("status").value("ATIVA")).andExpect(jsonPath("perfis.length()").value(1))
                .andExpect(jsonPath("perfis[0]").value("OPERADOR"))
                .andExpect(jsonPath("$.length()").value(6)).andReturn();
        semSegredos(resultado);
        String hash = jdbc.queryForObject("SELECT senha_hash FROM conta_acesso WHERE pessoa_id=?", String.class, outraPessoaId);
        assertTrue(hash.startsWith("{bcrypt}"));
        assertTrue(encoder.matches(SENHA, hash));
        mvc.perform(get(BASE + "/me").with(bearer(mvc, outroLogin, SENHA)))
                .andExpect(status().isOk()).andExpect(jsonPath("pessoaId").value(outraPessoaId));
    }

    @Test
    void loginDuplicadoNormalizadoDeveRetornar409() throws Exception {
        var resultado = mvc.perform(post(BASE).with(administrador()).contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro(outraPessoaId, " " + login.toUpperCase(java.util.Locale.ROOT) + " ", SENHA)))
                .andExpect(status().isConflict()).andExpect(jsonPath("status").value(409))
                .andExpect(jsonPath("message").value("Login já cadastrado")).andReturn();
        semSegredos(resultado);
    }

    @Test
    void segundaContaDaPessoaDeveRetornar409() throws Exception {
        var resultado = mvc.perform(post(BASE).with(administrador()).contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro(pessoaId, outroLogin, SENHA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("message").value("Pessoa já possui conta de acesso")).andReturn();
        semSegredos(resultado);
    }

    @Test
    void pessoaInexistenteDeveRetornar404() throws Exception {
        var resultado = mvc.perform(post(BASE).with(administrador()).contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro(Long.MAX_VALUE, outroLogin, SENHA)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("message").value("Pessoa não encontrada")).andReturn();
        semSegredos(resultado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "curta", "                                                                         ",
            "ééééééééééééééééééééééééééééééééééééé"})
    void cadastroComSenhaInvalidaDeveRetornar400SemExporValor(String senha) throws Exception {
        var resultado = mvc.perform(post(BASE).with(administrador()).contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro(outraPessoaId, outroLogin, senha)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("status").value(400)).andReturn();
        semSegredos(resultado);
        if (!senha.isBlank()) assertFalse(resultado.getResponse().getContentAsString().contains(senha));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM conta_acesso WHERE pessoa_id=?", Integer.class, outraPessoaId));
    }

    @Test
    void cadastroSemCamposObrigatoriosDeveRetornar400() throws Exception {
        var resultado = mvc.perform(post(BASE).with(administrador()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("fields.pessoaId").exists())
                .andExpect(jsonPath("fields.login").exists()).andExpect(jsonPath("fields.senha").exists()).andReturn();
        semSegredos(resultado);
    }

    @Test
    void cadastroAdministrativoNaoDeveAceitarEscaladaDePerfilOuStatusNoDto() throws Exception {
        String corpo = json.writeValueAsString(Map.of("pessoaId", outraPessoaId, "login", outroLogin, "senha", SENHA,
                "perfis", new String[]{"ADMINISTRADOR"}, "status", "BLOQUEADA", "senhaHash", "hash-injetado"));
        var resultado = mvc.perform(post(BASE).with(administrador()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("status").value("ATIVA"))
                .andExpect(jsonPath("perfis.length()").value(1)).andExpect(jsonPath("perfis[0]").value("OPERADOR")).andReturn();
        semSegredos(resultado);
        var salva = contas.buscarPorLogin(outroLogin);
        assertEquals(java.util.Set.of(com.curso.instrumento2026.domain.PerfilAcesso.OPERADOR), salva.getPerfis());
        assertTrue(encoder.matches(SENHA, salva.getSenhaHash()));
    }

    @Test
    void consultaDeveUsarPrincipalIgnorandoIdsELoginsInformadosPeloCliente() throws Exception {
        var outra = comoAdministrador(() -> contas.cadastrar(outraPessoaId, outroLogin, SENHA));
        var resultado = mvc.perform(get(BASE + "/me").with(bearer(mvc, login, SENHA))
                        .param("id", outra.getId().toString()).param("pessoaId", outraPessoaId.toString()).param("login", outroLogin))
                .andExpect(status().isOk()).andExpect(jsonPath("id").value(contaId))
                .andExpect(jsonPath("pessoaId").value(pessoaId)).andExpect(jsonPath("login").value(login))
                .andExpect(jsonPath("$.length()").value(6)).andReturn();
        semSegredos(resultado);
    }

    @Test
    void consultaETrocaAnonimasDevemRetornar401SemJsonComercial() throws Exception {
        mvc.perform(get(BASE + "/me")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer"))).andExpect(content().string(""));
        mvc.perform(put(BASE + "/me/senha").contentType(MediaType.APPLICATION_JSON).content(troca(SENHA, NOVA)))
                .andExpect(status().isUnauthorized()).andExpect(content().string(""));
    }

    @Test
    void trocaValidaDeveRetornar204EImpedirNovoLoginComSenhaAnteriorAposCommit() throws Exception {
        String emitido = token(mvc, login, SENHA);
        var resultado = mvc.perform(put(BASE + "/me/senha").with(bearer(emitido))
                        .contentType(MediaType.APPLICATION_JSON).content(troca(SENHA, NOVA)))
                .andExpect(status().isNoContent()).andExpect(content().string("")).andReturn();
        semSegredos(resultado);
        String hash = jdbc.queryForObject("SELECT senha_hash FROM conta_acesso WHERE id=?", String.class, contaId);
        assertTrue(encoder.matches(NOVA, hash));
        assertFalse(encoder.matches(SENHA, hash));
        mvc.perform(loginRequest(login, SENHA)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Login ou senha inválidos"));
        // A troca não revoga JWT já emitido: a validação do token permanece stateless.
        mvc.perform(get(BASE + "/me").with(bearer(emitido))).andExpect(status().isOk());
        mvc.perform(get(BASE + "/me").with(bearer(mvc, login, NOVA))).andExpect(status().isOk());
    }

    @Test
    void senhaAtualIncorretaDeveRetornar400SemAlterarHash() throws Exception {
        String hashAntes = contas.buscarPorId(contaId).getSenhaHash();
        var resultado = mvc.perform(put(BASE + "/me/senha").with(bearer(mvc, login, SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content(troca("Senha atual errada", NOVA)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").value("Senha atual inválida")).andReturn();
        semSegredos(resultado);
        assertEquals(hashAntes, contas.buscarPorId(contaId).getSenhaHash());
    }

    @Test
    void novaSenhaInvalidaOuRepetidaDeveRetornar400() throws Exception {
        for (String nova : new String[]{"curta", "é".repeat(37), SENHA}) {
            var resultado = mvc.perform(put(BASE + "/me/senha").with(bearer(mvc, login, SENHA))
                            .contentType(MediaType.APPLICATION_JSON).content(troca(SENHA, nova)))
                    .andExpect(status().isBadRequest()).andReturn();
            semSegredos(resultado);
        }
        assertTrue(encoder.matches(SENHA, contas.buscarPorId(contaId).getSenhaHash()));
    }

    @Test
    void idsNoCorpoOuQueryNaoDevemPermitirTrocarSenhaDeOutraConta() throws Exception {
        var outra = comoAdministrador(() -> contas.cadastrar(outraPessoaId, outroLogin, "Senha exclusiva outra 2026"));
        String hashOutra = outra.getSenhaHash();
        String corpo = json.writeValueAsString(Map.of("id", outra.getId(), "contaId", outra.getId(),
                "pessoaId", outraPessoaId, "login", outroLogin, "senhaAtual", SENHA, "novaSenha", NOVA));
        mvc.perform(put(BASE + "/me/senha").param("id", outra.getId().toString()).param("login", outroLogin)
                        .with(bearer(mvc, login, SENHA)).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNoContent());
        assertEquals(hashOutra, contas.buscarPorId(outra.getId()).getSenhaHash());
        assertTrue(encoder.matches(NOVA, contas.buscarPorId(contaId).getSenhaHash()));
    }

    @Test
    void senhaAtualDeOutraPessoaNaoDeveAutorizarTrocaMesmoInformandoSeuLogin() throws Exception {
        var outra = comoAdministrador(() -> contas.cadastrar(outraPessoaId, outroLogin, "Senha exclusiva outra 2026"));
        String corpo = json.writeValueAsString(Map.of("login", outroLogin,
                "senhaAtual", "Senha exclusiva outra 2026", "novaSenha", NOVA));
        mvc.perform(put(BASE + "/me/senha").with(bearer(mvc, login, SENHA)).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").value("Senha atual inválida"));
        assertTrue(encoder.matches(SENHA, contas.buscarPorId(contaId).getSenhaHash()));
        assertEquals(outra.getSenhaHash(), contas.buscarPorId(outra.getId()).getSenhaHash());
    }

    @Test
    void rotasPorIdESenhaContinuamAusentesEOperacoesAdministrativasExigemAdministrador() throws Exception {
        mvc.perform(get(BASE + "/{id}", contaId).with(bearer(mvc, login, SENHA))).andExpect(status().isNotFound());
        mvc.perform(put(BASE + "/{id}/senha", contaId).with(bearer(mvc, login, SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content(troca(SENHA, NOVA))).andExpect(status().isNotFound());
        mvc.perform(put(BASE + "/{id}/status", contaId).with(bearer(mvc, login, SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BLOQUEADA\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(BASE + "/{id}/perfis", contaId).with(bearer(mvc, login, SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"perfis\":[\"OPERADOR\",\"ADMINISTRADOR\"]}"))
                .andExpect(status().isForbidden());
    }

    private void acessoProprioNegado() throws Exception {
        mvc.perform(get(BASE + "/me"))
                .andExpect(status().isUnauthorized()).andExpect(content().string(""));
        mvc.perform(put(BASE + "/me/senha")
                        .contentType(MediaType.APPLICATION_JSON).content(troca(SENHA, NOVA)))
                .andExpect(status().isUnauthorized()).andExpect(content().string(""));
        mvc.perform(loginRequest(login, SENHA)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Login ou senha inválidos"))
                .andExpect(jsonPath("accessToken").doesNotExist());
    }

    @Test
    void contaBloqueadaNaoDeveObterTokenParaConsultarOuTrocarSenha() throws Exception {
        comoAdministrador(() -> contas.alterarStatus(contaId, StatusContaAcesso.BLOQUEADA));
        acessoProprioNegado();
        assertTrue(encoder.matches(SENHA, contas.buscarPorId(contaId).getSenhaHash()));
    }

    @Test
    void pessoaInativaNaoDeveObterTokenParaConsultarOuTrocarSenha() throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> pessoas.findById(pessoaId).orElseThrow().inativar());
        acessoProprioNegado();
    }

    @Test
    void cadastroParaPessoaInativaDeveSerRejeitado() throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> pessoas.findById(outraPessoaId).orElseThrow().inativar());
        mvc.perform(post(BASE).with(administrador()).contentType(MediaType.APPLICATION_JSON).content(cadastro(outraPessoaId, outroLogin, SENHA)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").value("Pessoa deve estar ativa"));
    }

    @Test
    void jsonMalformadoNaoDeveExporSenhasNoErro() throws Exception {
        var resultado = mvc.perform(put(BASE + "/me/senha").with(bearer(mvc, login, SENHA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"senhaAtual\":\"" + SENHA + "\",\"novaSenha\":"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").value("JSON inválido ou malformado")).andReturn();
        semSegredos(resultado);
    }

    @Test
    void novasOperacoesNaoDevemAlterarRotasOuSchemasHistoricos() throws Exception {
        String yaml = mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        com.curso.instrumento2026.support.ContratoOpenApiSupport.verificarCompatibilidadeComAula10(
                io.swagger.v3.core.util.Yaml.mapper().readTree(yaml));
    }
}
