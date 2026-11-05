package com.curso.instrumento2026;

import com.curso.instrumento2026.api.ContaAcessoController;
import com.curso.instrumento2026.api.mapper.ContaAcessoMapper;
import com.curso.instrumento2026.config.JwtConfig;
import com.curso.instrumento2026.config.JwtProperties;
import com.curso.instrumento2026.config.PasswordEncoderConfig;
import com.curso.instrumento2026.config.SecurityConfig;
import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.repository.ContaAcessoRepository;
import com.curso.instrumento2026.repository.PessoaRepository;
import com.curso.instrumento2026.service.ContaAcessoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static com.curso.instrumento2026.support.JwtTestSupport.bearer;
import static com.curso.instrumento2026.support.JwtTestSupport.bearerComPerfis;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real controller, mapper, service and method-security proxy. Repository state is exclusively in memory.
@WebMvcTest(controllers = ContaAcessoController.class, properties = {
        "app.security.jwt.issuer=https://instrumento2026.local", "app.security.jwt.expiration=PT30M",
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@Import({SecurityConfig.class, JwtConfig.class, PasswordEncoderConfig.class, ContaAcessoService.class,
        ContaAcessoMapper.class, ContaAcessoAdministracaoTest.ContextoSemBanco.class})
class ContaAcessoAdministracaoTest {
    private static final String REACHED = ContaAcessoAdministracaoTest.class.getName() + ".afterSecurity";
    private static final String HASH = "hash-exclusivamente-ficticio";
    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @Autowired JwtProperties jwtProperties;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ContaAcessoService service;
    @Autowired ApplicationContext contexto;
    @MockitoBean ContaAcessoRepository contas;
    @MockitoBean PessoaRepository pessoas;
    private ContaAcesso atual;
    private ContaAcesso outra;
    private static final String CADASTRO = """
            {"pessoaId":99,"login":"nova.fixture","senha":"Senha fixture longa 2026"}
            """;

    @BeforeEach
    void prepararEntidadesSemBanco() {
        assertTrue(contexto.getBeansOfType(DataSource.class).isEmpty());
        assertTrue(AopUtils.isAopProxy(service));
        atual = conta(1L, "admin.atual");
        atual.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        outra = conta(2L, "outra.conta");
        when(contas.findOneById(1L)).thenReturn(Optional.of(atual));
        when(contas.findOneById(2L)).thenReturn(Optional.of(outra));
        when(contas.contarAdministradoresAtivos()).thenReturn(2L);
    }

    @Test
    void cadastroAnonimoDeveSerNegadoAntesDeExecutarControllerOuConsultarRepositories() throws Exception {
        var resultado = mvc.perform(post("/api/contas-acesso").contentType("application/json").content(CADASTRO))
                .andExpect(status().isUnauthorized()).andReturn();
        assertNull(resultado.getRequest().getAttribute(REACHED));
        verifyNoInteractions(contas, pessoas);
    }

    @Test
    void cadastroPorOperadorComJwtAssinadoDeveSerNegadoAntesDeEscrever() throws Exception {
        var resultado = mvc.perform(post("/api/contas-acesso")
                        .with(bearerComPerfis(encoder, jwtProperties, "operador.fixture", "OPERADOR"))
                        .contentType("application/json").content(CADASTRO))
                .andExpect(status().isForbidden()).andReturn();
        assertNull(resultado.getRequest().getAttribute(REACHED));
        verifyNoInteractions(contas, pessoas);
    }

    @Test
    void cadastroAdministrativoComJwtAssinadoDeveExecutarServiceSemVazarIdentidadeEntreRequests() throws Exception {
        var pessoa = new Pessoa("Pessoa nova fixture", "nova@example.com", "11144477735", LocalDate.now());
        ReflectionTestUtils.setField(pessoa, "id", 99L);
        when(pessoas.findById(99L)).thenReturn(Optional.of(pessoa));
        when(contas.saveAndFlush(any(ContaAcesso.class))).thenAnswer(invocacao -> {
            ContaAcesso salva = invocacao.getArgument(0);
            ReflectionTestUtils.setField(salva, "id", 3L);
            return salva;
        });
        assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        var resultado = mvc.perform(post("/api/contas-acesso")
                        .with(bearerComPerfis(encoder, jwtProperties, "administrador.fixture", "ADMINISTRADOR"))
                        .contentType("application/json").content(CADASTRO))
                .andExpect(status().isCreated()).andExpect(jsonPath("id").value(3))
                .andExpect(jsonPath("pessoaId").value(99)).andExpect(jsonPath("status").value("ATIVA"))
                .andExpect(jsonPath("perfis[0]").value("OPERADOR"))
                .andExpect(jsonPath("$.length()").value(6)).andReturn();
        assertEquals(Boolean.TRUE, resultado.getRequest().getAttribute(REACHED));
        assertFalse(resultado.getResponse().getContentAsString().contains("Senha fixture longa 2026"));
        assertFalse(resultado.getResponse().getContentAsString().contains("senhaHash"));
        var capturada = org.mockito.ArgumentCaptor.forClass(ContaAcesso.class);
        verify(contas).saveAndFlush(capturada.capture());
        assertTrue(passwordEncoder.matches("Senha fixture longa 2026", capturada.getValue().getSenhaHash()));
        assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        clearInvocations(contas, pessoas);
        mvc.perform(post("/api/contas-acesso").contentType("application/json").content(CADASTRO))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(contas, pessoas);
    }

    @Test
    void administradorContinuaSujeitoAValidacaoDoCadastroAntesDoService() throws Exception {
        var resultado = mvc.perform(post("/api/contas-acesso")
                        .with(bearerComPerfis(encoder, jwtProperties, "administrador.fixture", "ADMINISTRADOR"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("fields.pessoaId").exists())
                .andExpect(jsonPath("fields.login").exists()).andExpect(jsonPath("fields.senha").exists()).andReturn();
        assertEquals(Boolean.TRUE, resultado.getRequest().getAttribute(REACHED));
        verifyNoInteractions(contas, pessoas);
    }

    @Test
    void administradorDeveSubstituirPerfisDeOutraContaSemExporCredenciais() throws Exception {
        var resultado = alterar(2, "perfis", "{\"perfis\":[\"OPERADOR\",\"ADMINISTRADOR\"]}", token("ADMINISTRADOR"), 200);
        assertEquals(Set.of(PerfilAcesso.OPERADOR, PerfilAcesso.ADMINISTRADOR), outra.getPerfis());
        assertEquals(StatusContaAcesso.ATIVA, outra.getStatus());
        assertEquals(HASH, outra.getSenhaHash());
        var response = resultado.getResponse().getContentAsString();
        assertFalse(response.contains("senha")); assertFalse(response.contains(HASH));
        verify(contas).findOneById(2L);
    }

    @Test
    void administradorDeveBloquearEReativarOutraConta() throws Exception {
        alterar(2, "status", "{\"status\":\"BLOQUEADA\"}", token("ADMINISTRADOR"), 200);
        assertEquals(StatusContaAcesso.BLOQUEADA, outra.getStatus());
        alterar(2, "status", "{\"status\":\"ATIVA\"}", token("ADMINISTRADOR"), 200);
        assertEquals(StatusContaAcesso.ATIVA, outra.getStatus());
        assertEquals(HASH, outra.getSenhaHash());
    }

    @ParameterizedTest
    @ValueSource(strings = {"perfis", "status"})
    void anonimoDeveReceber401AntesDeExecutarController(String operacao) throws Exception {
        var resultado = mvc.perform(put("/api/contas-acesso/2/" + operacao)
                .contentType("application/json").content("{}")).andExpect(status().isUnauthorized()).andReturn();
        assertNull(resultado.getRequest().getAttribute(REACHED));
        verifyNoInteractions(contas, pessoas);
    }

    @ParameterizedTest
    @ValueSource(strings = {"perfis", "status"})
    void operadorDeveReceber403AntesDeValidacaoOuConsulta(String operacao) throws Exception {
        var resultado = mvc.perform(put("/api/contas-acesso/2/" + operacao).with(bearer(token("OPERADOR")))
                .contentType("application/json").content("{}")).andExpect(status().isForbidden()).andReturn();
        assertNull(resultado.getRequest().getAttribute(REACHED));
        verifyNoInteractions(contas, pessoas);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"perfis|{\"perfis\":[\"OPERADOR\"]}|próprio perfil",
            "status|{\"status\":\"BLOQUEADA\"}|própria conta"})
    void administradorNaoDeveRemoverSeuPerfilOuBloquearSeMesmo(String operacao, String json, String mensagem) throws Exception {
        var resultado = alterar(1, operacao, json, token("ADMINISTRADOR"), 400);
        assertTrue(resultado.getResponse().getContentAsString().contains(mensagem));
        assertTrue(atual.possuiPerfil(PerfilAcesso.ADMINISTRADOR));
        assertEquals(StatusContaAcesso.ATIVA, atual.getStatus());
        verify(contas, never()).contarAdministradoresAtivos();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"perfis|{\"perfis\":[\"OPERADOR\"]}", "status|{\"status\":\"BLOQUEADA\"}"})
    void ultimoAdministradorUtilizavelNaoPodePerderPerfilOuSerBloqueado(String operacao, String json) throws Exception {
        outra.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        // The caller retains a previously issued administrator JWT after losing its persisted role.
        atual.substituirPerfis(Set.of(PerfilAcesso.OPERADOR));
        when(contas.contarAdministradoresAtivos()).thenReturn(1L);
        alterar(2, operacao, json, token("ADMINISTRADOR"), 400);
        assertEquals(Set.of(PerfilAcesso.OPERADOR, PerfilAcesso.ADMINISTRADOR), outra.getPerfis());
        assertEquals(StatusContaAcesso.ATIVA, outra.getStatus());
        verify(contas).contarAdministradoresAtivos();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"perfis|{\"perfis\":[\"OPERADOR\"]}", "status|{\"status\":\"BLOQUEADA\"}"})
    void outroAdministradorPodePerderPerfilOuSerBloqueadoQuandoHaAlternativa(String operacao, String json) throws Exception {
        outra.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        alterar(2, operacao, json, token("ADMINISTRADOR"), 200);
        verify(contas).contarAdministradoresAtivos();
        if (operacao.equals("perfis")) assertEquals(Set.of(PerfilAcesso.OPERADOR), outra.getPerfis());
        else assertEquals(StatusContaAcesso.BLOQUEADA, outra.getStatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void administradorJaInutilizavelNaoDeveSerContadoComoUltimoDisponivel(boolean pessoaInativa) throws Exception {
        outra.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        if (pessoaInativa) outra.getPessoa().inativar(); else outra.bloquear();
        when(contas.contarAdministradoresAtivos()).thenReturn(1L);
        alterar(2, "perfis", "{\"perfis\":[\"OPERADOR\"]}", token("ADMINISTRADOR"), 200);
        assertFalse(outra.possuiPerfil(PerfilAcesso.ADMINISTRADOR));
        verify(contas, never()).contarAdministradoresAtivos();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"perfis|{}", "perfis|{\"perfis\":[]}", "perfis|{\"perfis\":null}",
            "perfis|{\"perfis\":[null]}", "perfis|{\"perfis\":[\"INVENTADO\"]}",
            "status|{}", "status|{\"status\":null}", "status|{\"status\":\"ATIVO\"}"})
    void dtoInvalidoDeveRetornar400SemExecutarService(String operacao, String json) throws Exception {
        alterar(2, operacao, json, token("ADMINISTRADOR"), 400);
        verifyNoInteractions(contas, pessoas);
        assertEquals(Set.of(PerfilAcesso.OPERADOR), outra.getPerfis());
        assertEquals(StatusContaAcesso.ATIVA, outra.getStatus());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"perfis|{\"perfis\":[\"OPERADOR\"]}", "status|{\"status\":\"ATIVA\"}"})
    void contaInexistenteDeveContinuarRetornando404DeNegocio(String operacao, String json) throws Exception {
        alterar(999, operacao, json, token("ADMINISTRADOR"), 404);
        verify(contas).findOneById(999L);
    }

    @Test
    void camposDeIdentidadeFornecidosNoJsonNaoDevemContornarAutoprotecao() throws Exception {
        alterar(1, "perfis", "{\"perfis\":[\"OPERADOR\"],\"login\":\"outra.conta\",\"id\":2}", token("ADMINISTRADOR"), 400);
        assertTrue(atual.possuiPerfil(PerfilAcesso.ADMINISTRADOR));
    }

    private org.springframework.test.web.servlet.MvcResult alterar(long id, String operacao, String json, String token, int status) throws Exception {
        var resultado = mvc.perform(put("/api/contas-acesso/" + id + "/" + operacao).with(bearer(token))
                .contentType("application/json").content(json)).andExpect(status().is(status)).andReturn();
        assertEquals(Boolean.TRUE, resultado.getRequest().getAttribute(REACHED));
        if (status >= 400) assertTrue(resultado.getResponse().getContentAsString().contains("\"status\":" + status));
        else assertFalse(resultado.getResponse().getContentAsString().contains(HASH));
        return resultado;
    }

    private String token(String perfil) {
        Instant agora = Instant.now();
        return encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder().issuer("https://instrumento2026.local")
                .subject("admin.atual").issuedAt(agora).expiresAt(agora.plusSeconds(1800))
                .claim("perfis", Set.of(perfil)).build())).getTokenValue();
    }

    private ContaAcesso conta(long id, String login) {
        var pessoa = new Pessoa("Pessoa ficticia " + id, "pessoa" + id + "@example.com", "52998224725", LocalDate.now());
        ReflectionTestUtils.setField(pessoa, "id", id + 10);
        var conta = new ContaAcesso(pessoa, login, HASH);
        ReflectionTestUtils.setField(conta, "id", id);
        return conta;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ContextoSemBanco implements WebMvcConfigurer {
        @Bean
        InMemoryUserDetailsManager usuariosFicticios(PasswordEncoder encoder) {
            return new InMemoryUserDetailsManager(User.withUsername("usuario.ficticio")
                    .password(encoder.encode("senha ficticia administrativa")).roles("OPERADOR").build());
        }
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new HandlerInterceptor() {
                @Override
                public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                    request.setAttribute(REACHED, Boolean.TRUE);
                    return true;
                }
            });
        }
    }
}
