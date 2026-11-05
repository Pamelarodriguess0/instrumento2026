package com.curso.instrumento2026;

import com.curso.instrumento2026.api.*;
import com.curso.instrumento2026.api.dto.ContaAcessoResponse;
import com.curso.instrumento2026.api.mapper.*;
import com.curso.instrumento2026.config.*;
import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static com.curso.instrumento2026.support.JwtTestSupport.bearer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real controllers and security filters; all business persistence boundaries are mocked.
@WebMvcTest(controllers = {AutenticacaoController.class, HealthController.class, ContaAcessoController.class,
        PessoaController.class, ClienteController.class, ColaboradorController.class, CategoriaInstrumentoController.class,
        FornecedorController.class, InstrumentoController.class, VendaController.class}, properties = {
        "app.security.jwt.issuer=https://instrumento2026.local", "app.security.jwt.expiration=PT30M",
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@Import({SecurityConfig.class, JwtConfig.class, PasswordEncoderConfig.class, AutenticacaoService.class,
        TokenService.class, PoliticaHttpSecurityTest.ContextoSemBanco.class})
class PoliticaHttpSecurityTest {
    private static final String REACHED = PoliticaHttpSecurityTest.class.getName() + ".afterSecurity";
    private static final String CONTA = "{\"pessoaId\":2,\"login\":\"matriz.usuario\",\"senha\":\"senha ficticia cadastro\"}";
    private static final String PESSOA = "{\"nome\":\"Pessoa ficticia\",\"email\":\"matriz@example.com\",\"cpf\":\"52998224725\"}";
    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @Autowired ApplicationContext contexto;
    @MockitoBean ContaAcessoService contas;
    @MockitoBean PessoaService pessoas;
    @MockitoBean ClienteService clientes;
    @MockitoBean ColaboradorService colaboradores;
    @MockitoBean CategoriaInstrumentoService categorias;
    @MockitoBean FornecedorService fornecedores;
    @MockitoBean InstrumentoService instrumentos;
    @MockitoBean VendaService vendas;
    @MockitoBean ContaAcessoMapper contaMapper;
    @MockitoBean PessoaMapper pessoaMapper;
    @MockitoBean ClienteMapper clienteMapper;
    @MockitoBean ColaboradorMapper colaboradorMapper;
    @MockitoBean CategoriaInstrumentoMapper categoriaMapper;
    @MockitoBean FornecedorMapper fornecedorMapper;
    @MockitoBean InstrumentoMapper instrumentoMapper;
    @MockitoBean VendaMapper vendaMapper;

    @BeforeEach
    void prepararSomenteObjetosEmMemoria() {
        assertTrue(contexto.getBeansOfType(DataSource.class).isEmpty());
        ContaAcesso conta = mock(ContaAcesso.class);
        when(contas.cadastrar(2L, "matriz.usuario", "senha ficticia cadastro")).thenReturn(conta);
        when(contas.buscarPorLogin("matriz.usuario")).thenReturn(conta);
        when(contaMapper.toResponse(conta)).thenReturn(new ContaAcessoResponse(1L, 2L, "Pessoa ficticia",
                "matriz.usuario", StatusContaAcesso.ATIVA, Set.of(PerfilAcesso.OPERADOR)));
        Pessoa pessoa = mock(Pessoa.class);
        when(pessoa.getId()).thenReturn(2L);
        when(pessoaMapper.toEntity(any())).thenReturn(pessoa);
        when(pessoas.cadastrar(pessoa)).thenReturn(pessoa);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/pessoas/1", "/api/clientes/1", "/api/colaboradores/1", "/api/categorias/1",
            "/api/fornecedores/1", "/api/instrumentos/1", "/api/vendas/1", "/api/contas-acesso/me"})
    void anonimoNaoDeveAtingirControllersProtegidos(String rota) throws Exception {
        bloqueado(get(rota), 401);
    }

    @ParameterizedTest
    @CsvSource({"OPERADOR,pessoas", "ADMINISTRADOR,pessoas", "OPERADOR,clientes", "ADMINISTRADOR,clientes",
            "OPERADOR,colaboradores", "ADMINISTRADOR,colaboradores", "OPERADOR,categorias", "ADMINISTRADOR,categorias",
            "OPERADOR,fornecedores", "ADMINISTRADOR,fornecedores", "OPERADOR,instrumentos", "ADMINISTRADOR,instrumentos",
            "OPERADOR,vendas", "ADMINISTRADOR,vendas", "OPERADOR,contas-acesso/me", "ADMINISTRADOR,contas-acesso/me"})
    void perfisPermitidosDevemAtingirConsultasComerciais(String perfil, String recurso) throws Exception {
        String rota = "/api/" + recurso + (recurso.equals("contas-acesso/me") ? "" : "/1");
        permitido(get(rota).with(bearer(token(perfil))), 200);
        switch (recurso) {
            case "pessoas" -> verify(pessoas).buscarPorId(1L);
            case "clientes" -> verify(clientes).buscarPorId(1L);
            case "colaboradores" -> verify(colaboradores).buscarPorId(1L);
            case "categorias" -> verify(categorias).buscarPorId(1L);
            case "fornecedores" -> verify(fornecedores).buscarPorId(1L);
            case "instrumentos" -> verify(instrumentos).buscarPorId(1L);
            case "vendas" -> verify(vendas).buscarPorId(1L);
            case "contas-acesso/me" -> verify(contas).buscarPorLogin("matriz.usuario");
            default -> fail("Recurso sem verificação de chamada");
        }
    }

    @Test
    void criacaoDeContaDeveSerExclusivaDoAdministrador() throws Exception {
        bloqueado(json(post("/api/contas-acesso"), CONTA), 401);
        bloqueado(json(post("/api/contas-acesso").with(bearer(token("OPERADOR"))), CONTA), 403);
        permitido(json(post("/api/contas-acesso").with(bearer(token("ADMINISTRADOR"))), CONTA), 201);
        verify(contas).cadastrar(2L, "matriz.usuario", "senha ficticia cadastro");
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERADOR", "ADMINISTRADOR"})
    void cadastroDePessoaDeveSerProtegidoMasPermitidoAosDoisPerfis(String perfil) throws Exception {
        bloqueado(json(post("/api/pessoas"), PESSOA), 401);
        permitido(json(post("/api/pessoas").with(bearer(token(perfil))), PESSOA), 201);
        verify(pessoas).cadastrar(any(Pessoa.class));
    }

    @ParameterizedTest
    @CsvSource({"/api/categorias/1,204", "/api/instrumentos/1,204", "/api/vendas/1/itens/2,200"})
    void todasAsExclusoesReaisDevemExigirAdministrador(String rota, int sucesso) throws Exception {
        bloqueado(delete(rota), 401);
        bloqueado(delete(rota).with(bearer(token("OPERADOR"))), 403);
        permitido(delete(rota).with(bearer(token("ADMINISTRADOR"))), sucesso);
        if (rota.startsWith("/api/categorias")) verify(categorias).excluir(1L);
        else if (rota.startsWith("/api/instrumentos")) verify(instrumentos).excluir(1L);
        else verify(vendas).removerItem(1L, 2L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/contas-acesso/1/perfis", "/api/contas-acesso/1/status"})
    void caminhosAdministrativosDevemRecusarOperadorAntesDeValidarCorpo(String rota) throws Exception {
        bloqueado(json(put(rota).with(bearer(token("OPERADOR"))), "{}"), 403);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERADOR", "ADMINISTRADOR"})
    void trocaDaPropriaSenhaDeveSerPermitidaComSubjectDoToken(String perfil) throws Exception {
        permitido(json(put("/api/contas-acesso/me/senha").with(bearer(token(perfil))),
                "{\"senhaAtual\":\"senha ficticia atual\",\"novaSenha\":\"senha ficticia nova\"}"), 204);
        verify(contas).alterarSenha("matriz.usuario", "senha ficticia atual", "senha ficticia nova");
    }

    @ParameterizedTest
    @CsvSource({"OPERADOR,entradas", "ADMINISTRADOR,entradas", "OPERADOR,saidas", "ADMINISTRADOR,saidas"})
    void movimentacoesDeEstoqueDevemPermanecerOperacoesComerciais(String perfil, String movimento) throws Exception {
        permitido(json(post("/api/instrumentos/1/estoque/" + movimento).with(bearer(token(perfil))),
                "{\"quantidade\":1}"), 200);
        if (movimento.equals("entradas")) verify(instrumentos).receberEstoque(1L, BigDecimal.ONE);
        else verify(instrumentos).retirarEstoque(1L, BigDecimal.ONE);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void semPerfisOuComListaVaziaDeveSer403MesmoComScopeOuFatorBearer(boolean ausente) throws Exception {
        Instant agora = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("https://instrumento2026.local").subject("matriz.usuario")
                .issuedAt(agora).expiresAt(agora.plusSeconds(1800)).claim("scope", "OPERADOR ADMINISTRADOR");
        if (!ausente) claims.claim("perfis", List.of());
        String token = encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
        bloqueado(get("/api/contas-acesso/me").with(bearer(token)), 403);
        bloqueado(get("/api/instrumentos/1").with(bearer(token)), 403);
        bloqueado(json(post("/api/pessoas").with(bearer(token)), PESSOA), 403);
        bloqueado(json(post("/api/contas-acesso").with(bearer(token)), CONTA), 403);
        bloqueado(delete("/api/vendas/1/itens/2").with(bearer(token)), 403);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERADOR", "ADMINISTRADOR"})
    void caminhosForaDaApiDevemSerNegadosPorPadrao(String perfil) throws Exception {
        bloqueado(get("/fora-da-api").with(bearer(token(perfil))), 403);
    }

    @Test
    void basicDeveContinuarSemAutenticar() throws Exception {
        bloqueado(get("/api/instrumentos/1").with(httpBasic("matriz.usuario", "senha ficticia matriz")), 401);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/v3/api-docs.yaml",
            "/swagger-ui.html", "/swagger-ui/index.html", "/swagger-ui/swagger-ui-bundle.js"})
    void caminhosDeDocumentacaoDevemPassarPelosFiltrosAnonimamente(String rota) throws Exception {
        // This MVC slice has no Springdoc generation. Reaching MVC proves the filter permitted the request.
        var resultado = mvc.perform(get(rota)).andReturn();
        assertEquals(Boolean.TRUE, resultado.getRequest().getAttribute(REACHED));
        assertNotEquals(401, resultado.getResponse().getStatus());
        assertNotEquals(403, resultado.getResponse().getStatus());
        semServicosExecutados();
    }

    private String token(String perfil) {
        Instant agora = Instant.now();
        return encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder().issuer("https://instrumento2026.local")
                .subject("matriz.usuario").issuedAt(agora).expiresAt(agora.plusSeconds(1800))
                .claim("perfis", List.of(perfil)).build())).getTokenValue();
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void bloqueado(MockHttpServletRequestBuilder request, int status) throws Exception {
        MvcResult resultado = mvc.perform(request).andExpect(status().is(status)).andReturn();
        assertNull(resultado.getRequest().getAttribute(REACHED), "O filtro deve bloquear antes do MVC");
        semServicosExecutados();
    }

    private void permitido(MockHttpServletRequestBuilder request, int status) throws Exception {
        MvcResult resultado = mvc.perform(request).andExpect(status().is(status)).andReturn();
        assertEquals(Boolean.TRUE, resultado.getRequest().getAttribute(REACHED));
    }

    private void semServicosExecutados() {
        verifyNoInteractions(contas, pessoas, clientes, colaboradores, categorias, fornecedores, instrumentos, vendas);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ContextoSemBanco implements WebMvcConfigurer {
        @Bean
        InMemoryUserDetailsManager usuariosFicticios(PasswordEncoder encoder) {
            return new InMemoryUserDetailsManager(User.withUsername("matriz.usuario")
                    .password(encoder.encode("senha ficticia matriz")).roles("OPERADOR").build());
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
