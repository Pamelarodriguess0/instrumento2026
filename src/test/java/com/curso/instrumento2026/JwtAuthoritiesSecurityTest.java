package com.curso.instrumento2026;

import com.curso.instrumento2026.api.AutenticacaoController;
import com.curso.instrumento2026.api.ContaAcessoController;
import com.curso.instrumento2026.api.HealthController;
import com.curso.instrumento2026.api.dto.ContaAcessoResponse;
import com.curso.instrumento2026.api.mapper.ContaAcessoMapper;
import com.curso.instrumento2026.config.JwtConfig;
import com.curso.instrumento2026.config.PasswordEncoderConfig;
import com.curso.instrumento2026.config.SecurityConfig;
import com.curso.instrumento2026.domain.ContaAcesso;
import com.curso.instrumento2026.domain.PerfilAcesso;
import com.curso.instrumento2026.domain.StatusContaAcesso;
import com.curso.instrumento2026.service.AutenticacaoService;
import com.curso.instrumento2026.service.ContaAcessoService;
import com.curso.instrumento2026.service.TokenService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Instant;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.curso.instrumento2026.support.JwtTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real security filters, encoder, decoder and AuthenticationManager; no datasource or database fixtures.
@WebMvcTest(controllers = {AutenticacaoController.class, ContaAcessoController.class, HealthController.class},
        properties = {
                "app.security.jwt.issuer=https://instrumento2026.local",
                "app.security.jwt.expiration=PT30M",
                // Public didactic key, exclusive to this isolated test context.
                "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        })
@Import({SecurityConfig.class, JwtConfig.class, PasswordEncoderConfig.class,
        AutenticacaoService.class, TokenService.class, JwtAuthoritiesSecurityTest.ContextoSemBanco.class})
class JwtAuthoritiesSecurityTest {
    private static final String LOGIN = "operador.authorities";
    private static final String SENHA = "Senha ficticia authorities 2026";
    private static final String AUTHENTICATION = JwtAuthoritiesSecurityTest.class.getName() + ".authentication";

    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    @Autowired JwtAuthenticationConverter converter;
    @Autowired ApplicationContext contexto;
    @MockitoBean ContaAcessoService contas;
    @MockitoBean ContaAcessoMapper mapper;

    @BeforeEach
    void configurarConsultaSemPersistencia() {
        assertTrue(contexto.getBeansOfType(DataSource.class).isEmpty(), "Este teste não deve acessar banco de dados");
        ContaAcesso conta = mock(ContaAcesso.class);
        when(contas.buscarPorLogin(LOGIN)).thenReturn(conta);
        when(contas.cadastrar(any(), any(), any())).thenReturn(conta);
        when(mapper.toResponse(conta)).thenReturn(new ContaAcessoResponse(1L, 2L, "Pessoa ficticia",
                LOGIN, StatusContaAcesso.ATIVA, Set.of(PerfilAcesso.OPERADOR)));
    }

    static Stream<Arguments> perfis() {
        return Stream.of(
                Arguments.of(List.of("OPERADOR"), Set.of("ROLE_OPERADOR")),
                Arguments.of(List.of("ADMINISTRADOR"), Set.of("ROLE_ADMINISTRADOR")),
                Arguments.of(List.of("OPERADOR", "ADMINISTRADOR"), Set.of("ROLE_OPERADOR", "ROLE_ADMINISTRADOR")),
                Arguments.of(null, Set.of()),
                Arguments.of(List.of(), Set.of()));
    }

    @ParameterizedTest(name = "perfis={0}, authorities={1}")
    @MethodSource("perfis")
    void bearerDeveConverterSomentePerfisEPreservarSubject(List<String> perfis, Set<String> esperadas) throws Exception {
        Instant agora = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("https://instrumento2026.local").subject(LOGIN)
                .issuedAt(agora).expiresAt(agora.plusSeconds(1800))
                // Neither scope nor other name claims may supply roles or replace sub.
                .claim("scope", "ADMINISTRADOR OPERADOR").claim("preferred_username", "outra.identidade");
        if (perfis != null) claims.claim("perfis", perfis);
        String token = encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
        Set<String> esperadasNaAutenticacao = new HashSet<>(esperadas);
        // JwtAuthenticationConverter in Spring Security 7 also records the technical Bearer factor.
        esperadasNaAutenticacao.add("FACTOR_BEARER");
        var convertido = converter.convert(decoder.decode(token));
        assertNotNull(convertido);
        assertEquals(esperadasNaAutenticacao, authorities(convertido));
        assertEquals(LOGIN, convertido.getName());

        if (esperadas.isEmpty()) {
            mvc.perform(get("/api/contas-acesso/me").with(bearer(token))).andExpect(status().isForbidden());
            verifyNoInteractions(contas);
            return;
        }
        var resultado = mvc.perform(get("/api/contas-acesso/me").with(bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("login").value(LOGIN)).andReturn();
        Authentication autenticacao = (Authentication) resultado.getRequest().getAttribute(AUTHENTICATION);
        assertNotNull(autenticacao);
        assertTrue(autenticacao.isAuthenticated());
        assertEquals(LOGIN, autenticacao.getName());
        assertEquals(esperadasNaAutenticacao, authorities(autenticacao));
        verify(contas).buscarPorLogin(LOGIN);
    }

    @Test
    void loginDeveContinuarEmitindoJwtEIdentificandoContaComBearer() throws Exception {
        var resposta = mvc.perform(loginRequest("  OPERADOR.AUTHORITIES  ", SENHA)).andExpect(status().isOk())
                .andExpect(jsonPath("tokenType").value("Bearer"))
                .andExpect(jsonPath("expiresIn").value(1800)).andReturn();
        String token = JsonPath.read(resposta.getResponse().getContentAsString(), "$.accessToken");
        assertEquals(LOGIN, decoder.decode(token).getSubject());
        assertEquals(List.of("OPERADOR"), decoder.decode(token).getClaimAsStringList("perfis"));
        var me = mvc.perform(get("/api/contas-acesso/me").with(bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("login").value(LOGIN)).andReturn();
        assertEquals(Set.of("ROLE_OPERADOR", "FACTOR_BEARER"), authorities(
                (Authentication) me.getRequest().getAttribute(AUTHENTICATION)));
        assertNull(resposta.getRequest().getSession(false));
        assertNull(me.getRequest().getSession(false));
    }

    @Test
    void senhaIncorretaDeveContinuarRetornando401Generico() throws Exception {
        mvc.perform(loginRequest(LOGIN, "Senha ficticia incorreta"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("status").value(401))
                .andExpect(jsonPath("message").value("Login ou senha inválidos"))
                .andExpect(jsonPath("accessToken").doesNotExist());
    }

    @Test
    void ausenciaDeTokenEBasicDevemContinuarRejeitados() throws Exception {
        mvc.perform(get("/api/contas-acesso/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/contas-acesso/me").with(httpBasic(LOGIN, SENHA)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(contas);
    }

    @Test
    void trocaDeSenhaDeveContinuarUsandoIdentidadeDoBearer() throws Exception {
        String token = token(mvc, LOGIN, SENHA);
        mvc.perform(put("/api/contas-acesso/me/senha").with(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\":\"senha ficticia atual\",\"novaSenha\":\"senha ficticia nova\"}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(contas).alterarSenha(LOGIN, "senha ficticia atual", "senha ficticia nova");
    }

    @Test
    void healthDevePermanecerPublicoECadastroDeContaDeveExigirAutenticacao() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(content().string("OK"));
        mvc.perform(post("/api/contas-acesso").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pessoaId\":2,\"login\":\"operador.authorities\",\"senha\":\"senha ficticia cadastro\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(contas);
    }

    private static Set<String> authorities(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ContextoSemBanco implements WebMvcConfigurer {
        @Bean
        InMemoryUserDetailsManager usuariosFicticios(PasswordEncoder encoder) {
            return new InMemoryUserDetailsManager(User.withUsername(LOGIN)
                    .password(encoder.encode(SENHA)).roles("OPERADOR").build());
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new HandlerInterceptor() {
                @Override
                public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                    request.setAttribute(AUTHENTICATION, SecurityContextHolder.getContext().getAuthentication());
                    return true;
                }
            });
        }
    }
}
