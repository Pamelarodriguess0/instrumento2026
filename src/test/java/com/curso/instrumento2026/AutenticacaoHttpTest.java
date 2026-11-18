package com.curso.instrumento2026;

import com.curso.instrumento2026.api.AutenticacaoController;
import com.curso.instrumento2026.api.dto.*;
import com.curso.instrumento2026.config.*;
import com.curso.instrumento2026.exception.RefreshTokenInvalidoException;
import com.curso.instrumento2026.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=AutenticacaoController.class,properties={
        "app.security.jwt.issuer=https://instrumento2026.local", "app.security.jwt.expiration=PT30M",
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@Import({SecurityConfig.class,JwtConfig.class,AutenticacaoHttpTest.ContextoSemBanco.class})
class AutenticacaoHttpTest {
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;
    @Autowired JwtEncoder encoder;
    @MockitoBean AutenticacaoService login;
    @MockitoBean SessaoAutenticacaoService sessoes;
    private TokensAutenticacao par;
    @BeforeEach void preparar() {
        assertTrue(context.getBeansOfType(DataSource.class).isEmpty());
        par=new TokensAutenticacao(new TokenAcesso("access-sintetico",Instant.parse("2026-11-17T19:00:00Z"),1800),
                "refresh-sintetico",Instant.parse("2026-11-18T02:30:00Z"));
    }
    @Test void deveEntregarMesmoContratoNoLoginENaRenovacaoPublicos() throws Exception {
        when(login.autenticarComSessao("operador","senha-sintetica")).thenReturn(par);
        when(sessoes.renovar("refresh-sintetico")).thenReturn(par);
        for(var request:List.of(
                post("/api/autenticacao/login").content("{\"login\":\"operador\",\"senha\":\"senha-sintetica\"}"),
                post("/api/autenticacao/refresh").content("{\"refreshToken\":\"refresh-sintetico\"}"))) {
            var resultado=mvc.perform(request.contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6))
                    .andExpect(jsonPath("$.accessToken").value("access-sintetico"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(1800))
                    .andExpect(jsonPath("$.expiresAt").value("2026-11-17T19:00:00Z"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-sintetico"))
                    .andExpect(jsonPath("$.refreshTokenExpiresAt").value("2026-11-18T02:30:00Z"))
                    .andReturn();
            assertNull(resultado.getRequest().getSession(false));
        }
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"refreshToken\":null}","{\"refreshToken\":\"\"}","{\"refreshToken\":\" \"}","{\"refreshToken\":\"desconhecido\"}",""})
    void deveRetornarApiErrorGenericoParaRefreshInvalido(String corpo) throws Exception {
        when(sessoes.renovar(nullable(String.class))).thenThrow(new RefreshTokenInvalidoException());
        mvc.perform(post("/api/autenticacao/refresh").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh token inválido ou expirado"))
                .andExpect(jsonPath("$.path").value("/api/autenticacao/refresh"))
                .andExpect(jsonPath("$.fields").isEmpty()).andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"{\"refreshToken\":\"desconhecido\"}","{}",""})
    void logoutPublicoDeveResponder204SemCorpo(String corpo) throws Exception {
        mvc.perform(post("/api/autenticacao/logout").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
    }
    @Test void logoutGlobalDeveExigirBearerEUsarApenasSubject() throws Exception {
        mvc.perform(post("/api/autenticacao/logout-todos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/autenticacao/logout-todos").header("Authorization","Bearer invalido"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sessoes);
        var agora=Instant.now();
        String bearer=encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .issuer("https://instrumento2026.local").subject("propria.conta")
                .issuedAt(agora).expiresAt(agora.plusSeconds(1800)).build())).getTokenValue();
        mvc.perform(post("/api/autenticacao/logout-todos").header("Authorization","Bearer "+bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contaId\":999,\"login\":\"outra.conta\"}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(sessoes).encerrarTodas("propria.conta");
        verifyNoMoreInteractions(sessoes);
    }
    @Test void devePreservar400ParaJsonMalformadoELoginInvalido() throws Exception {
        mvc.perform(post("/api/autenticacao/refresh").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(login,sessoes);
    }
    @Test void dtosNaoDevemExporCredenciaisNoToString() {
        var response=new TokenResponse("access-sintetico","Bearer",1800,par.accessToken().expiraEm(),
                par.refreshToken(),par.refreshTokenExpiresAt());
        assertFalse(response.toString().contains(par.refreshToken()));
        assertFalse(response.toString().contains("access-sintetico"));
        assertFalse(new RefreshTokenRequest(par.refreshToken()).toString().contains(par.refreshToken()));
    }

    @TestConfiguration(proxyBeanMethods=false)
    static class ContextoSemBanco {
        @Bean
        InMemoryUserDetailsManager usuariosFicticios() {
            // O login é mockado neste slice; nenhum usuário é necessário.
            // O bean permite construir o manager global sem fallback lazy autorreferente.
            return new InMemoryUserDetailsManager();
        }
    }
}
