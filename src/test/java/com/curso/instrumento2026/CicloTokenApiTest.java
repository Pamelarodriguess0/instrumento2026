package com.curso.instrumento2026;

import com.curso.instrumento2026.api.dto.*;
import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.service.*;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.*;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Filtros, autenticação, assinatura, serviços e PostgreSQL reais; sem @Transactional de teste. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@ExtendWith(OutputCaptureExtension.class)
class CicloTokenApiTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtDecoder decoder;
    @Autowired Environment environment;
    @MockitoBean Clock clock;
    private TransactionTemplate tx;
    private Instant inicio;
    private final List<Fixture> fixtures=new ArrayList<>();
    private final List<String> segredos=new ArrayList<>();
    private static final String SENHA="senha-sintetica-ciclo-http";

    private record Fixture(Long contaId,Long pessoaId,String login) {}
    private record Par(String access,String refresh,Instant expira,Instant absoluto) {
        @Override public String toString(){return "Par[credenciais=[PROTEGIDAS]]";}
    }

    @BeforeEach void preparar() {
        assertTrue(Arrays.asList(environment.getActiveProfiles()).contains("test"));
        String destino="jdbc:postgresql://127.0.0.1:5432/instrumento2026_test";
        assertEquals(destino,environment.getProperty("spring.datasource.url"));
        assertEquals(destino,environment.getProperty("spring.liquibase.url"));
        assertEquals("public",environment.getProperty("spring.datasource.hikari.schema"));
        assertEquals("public",environment.getProperty("spring.liquibase.default-schema"));
        assertEquals("optional:file:./target/aula16-no-external-config.properties",environment.getProperty("spring.config.import"));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        tx=new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setTimeout(20);
        inicio=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        when(clock.instant()).thenReturn(inicio);
        novaTransacao(() -> {
            Object[] destinoReal=(Object[])em.createNativeQuery("select current_database(),host(inet_server_addr()),inet_server_port(),current_schema()").getSingleResult();
            assertEquals("instrumento2026_test",destinoReal[0]);
            assertEquals("127.0.0.1",destinoReal[1]);
            assertEquals(5432,((Number)destinoReal[2]).intValue());
            assertEquals("public",destinoReal[3]);
            assertEquals(44,((Number)em.createNativeQuery("select count(*) from databasechangelog").getSingleResult()).intValue());
            return null;
        });
    }

    @AfterEach void limparEConferirSaida(CapturedOutput output) {
        try {
            if(tx!=null) novaTransacao(() -> {
                for(var f:fixtures) {
                    var conta=em.find(ContaAcesso.class,f.contaId());
                    if(conta!=null) em.remove(conta);
                    em.flush(); // FK CASCADE remove somente as sessões da própria fixture.
                    var pessoa=em.find(Pessoa.class,f.pessoaId());
                    if(pessoa!=null) em.remove(pessoa);
                }
                return null;
            });
        } finally {
            String saida=output.getAll();
            assertTrue(segredos.stream().noneMatch(saida::contains),"Saída capturada não deve conter tokens ou hashes gerados");
        }
    }
    private <T>T novaTransacao(Supplier<T> acao){return tx.execute(status -> acao.get());}
    private Fixture fixture() {
        String login="ciclo.http."+UUID.randomUUID();
        return novaTransacao(() -> {
            String cpf=fixtures.isEmpty()?"52998224725":"11144477735";
            var pessoa=new Pessoa("Pessoa ciclo HTTP",login+"@example.com",cpf,LocalDate.of(2026,11,17));
            em.persist(pessoa);
            var conta=new ContaAcesso(pessoa,login,encoder.encode(SENHA));
            em.persist(conta);
            var f=new Fixture(conta.getId(),pessoa.getId(),login);
            fixtures.add(f);
            return f;
        });
    }
    private List<SessaoRefreshToken> sessoes(Fixture f) {
        return em.createQuery("select s from SessaoRefreshToken s where s.conta.id=:id order by s.id",SessaoRefreshToken.class)
                .setParameter("id",f.contaId()).getResultList();
    }
    private static String hash(String valor) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
    }
    private MvcResult requisicao(MockHttpServletRequestBuilder request,int status) throws Exception {
        var resultado=mvc.perform(request).andReturn();
        assertEquals(status,resultado.getResponse().getStatus(),"Status HTTP do cenário");
        assertNull(resultado.getRequest().getSession(false));
        assertNull(resultado.getResponse().getCookie("JSESSIONID"));
        assertTrue(resultado.getResponse().getHeaders("Set-Cookie").stream().noneMatch(h -> h.contains("JSESSIONID")));
        return resultado;
    }
    private Par par(MvcResult resultado) throws Exception {
        Map<String,Object> json=JsonPath.read(resultado.getResponse().getContentAsString(),"$");
        assertEquals(Set.of("accessToken","tokenType","expiresIn","expiresAt","refreshToken","refreshTokenExpiresAt"),json.keySet());
        assertEquals("Bearer",json.get("tokenType"));
        assertEquals(1800,((Number)json.get("expiresIn")).intValue());
        var par=new Par((String)json.get("accessToken"),(String)json.get("refreshToken"),
                Instant.parse((String)json.get("expiresAt")),Instant.parse((String)json.get("refreshTokenExpiresAt")));
        segredos.add(par.access()); segredos.add(par.refresh()); segredos.add(hash(par.refresh()));
        assertEquals(32,Base64.getUrlDecoder().decode(par.refresh()).length);
        assertTrue(par.refresh().matches("[A-Za-z0-9_-]{43}"));
        var jwt=decoder.decode(par.access()); // Decoder real, relógio e assinatura reais do Resource Server.
        assertEquals(par.expira(),jwt.getExpiresAt());
        assertTrue(jwt.hasClaim("sid"));
        var dto=new TokenResponse(par.access(),"Bearer",1800,par.expira(),par.refresh(),par.absoluto());
        var interno=new TokensAutenticacao(new TokenAcesso(par.access(),par.expira(),1800),par.refresh(),par.absoluto());
        for(String texto:List.of(dto.toString(),interno.toString(),new RefreshTokenRequest(par.refresh()).toString()))
            assertTrue(segredos.stream().noneMatch(texto::contains),"Representação textual deve proteger credenciais");
        return par;
    }
    private Par login(Fixture f) throws Exception {
        return par(requisicao(post("/api/autenticacao/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\""+f.login()+"\",\"senha\":\""+SENHA+"\"}"),200));
    }
    private MockHttpServletRequestBuilder tokenRequest(String rota,String refresh) {
        return post("/api/autenticacao/"+rota).contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\""+refresh+"\"}");
    }
    private Par renovar(Par anterior) throws Exception {return par(requisicao(tokenRequest("refresh",anterior.refresh()),200));}
    private void erroRefresh(MockHttpServletRequestBuilder request) throws Exception {
        var r=requisicao(request,401);
        assertEquals(MediaType.APPLICATION_JSON_VALUE,r.getResponse().getContentType());
        Map<String,Object> json=JsonPath.read(r.getResponse().getContentAsString(),"$");
        assertEquals(Set.of("timestamp","status","error","message","path","fields"),json.keySet());
        assertEquals(401,((Number)json.get("status")).intValue());
        assertEquals("Unauthorized",json.get("error"));
        assertEquals("Refresh token inválido ou expirado",json.get("message"));
        assertEquals("/api/autenticacao/refresh",json.get("path"));
        assertTrue(((Map<?,?>)json.get("fields")).isEmpty());
        assertNotNull(json.get("timestamp"));
        assertTrue(segredos.stream().noneMatch(r.getResponse().getContentAsString()::contains));
        assertNull(r.getResponse().getHeader("WWW-Authenticate")); // Erro do controller, não rejeição do filtro.
    }
    private void logout(String refresh) throws Exception {
        assertTrue(requisicao(tokenRequest("logout",refresh),204).getResponse().getContentAsString().isEmpty());
    }
    private void consultarComBearer(Par p,Fixture f) throws Exception {
        var r=requisicao(get("/api/contas-acesso/me").header("Authorization","Bearer "+p.access()),200);
        String identidade=JsonPath.read(r.getResponse().getContentAsString(),"$.login");
        assertEquals(f.login(),identidade);
    }

    @Test void deveRotacionarEConfirmarRevogacaoDaFamiliaApos401() throws Exception {
        var f=fixture();
        var r1=login(f);
        String esperado=hash(r1.refresh());
        novaTransacao(() -> {
            var lista=sessoes(f);
            assertEquals(1,lista.size());
            assertTrue(esperado.equals(lista.getFirst().getTokenHash()));
            String registro=(String)em.createNativeQuery("select row_to_json(s)::text from sessao_refresh_token s where conta_acesso_id=:id")
                    .setParameter("id",f.contaId()).getSingleResult();
            assertFalse(registro.contains(r1.refresh()));
            return null;
        });
        var r2=renovar(r1);
        assertFalse(r1.refresh().equals(r2.refresh()));
        assertFalse(r1.access().equals(r2.access()));
        assertEquals(r1.absoluto(),r2.absoluto());
        assertTrue(decoder.decode(r1.access()).getClaimAsString("sid").equals(decoder.decode(r2.access()).getClaimAsString("sid")));
        novaTransacao(() -> {
            var lista=sessoes(f);
            assertEquals(2,lista.size());
            assertEquals(inicio,lista.getFirst().getUtilizadaEm());
            assertFalse(lista.getLast().foiUtilizada());
            assertTrue(lista.getFirst().getFamiliaId().equals(lista.getLast().getFamiliaId()));
            assertEquals(lista.getFirst().getExpiraEm(),lista.getLast().getExpiraEm());
            return null;
        });
        erroRefresh(tokenRequest("refresh",r1.refresh()));
        novaTransacao(() -> {
            var lista=sessoes(f);
            assertEquals(2,lista.size());
            assertTrue(lista.stream().allMatch(s -> s.foiRevogada() && s.getMotivoRevogacao()==MotivoRevogacaoRefreshToken.REUTILIZACAO));
            return null;
        });
        erroRefresh(tokenRequest("refresh",r2.refresh()));
    }

    @ParameterizedTest @ValueSource(strings={"","{}","{\"refreshToken\":null}","{\"refreshToken\":\"\"}","{\"refreshToken\":\" \"}","{\"refreshToken\":\"desconhecido\"}"})
    void deveRejeitarRefreshAusenteVazioOuDesconhecido(String corpo) throws Exception {
        var f=fixture();
        erroRefresh(post("/api/autenticacao/refresh").contentType(MediaType.APPLICATION_JSON).content(corpo));
        novaTransacao(() -> {assertTrue(sessoes(f).isEmpty());return null;});
    }

    @ParameterizedTest @EnumSource(value=MotivoRevogacaoRefreshToken.class,names={"INATIVIDADE","EXPIRACAO_ABSOLUTA"})
    void deveRejeitarExpiracaoNoInstanteLimite(MotivoRevogacaoRefreshToken motivo) throws Exception {
        var f=fixture(); var p=login(f);
        when(clock.instant()).thenReturn(inicio.plusSeconds(motivo==MotivoRevogacaoRefreshToken.INATIVIDADE?1800:28800));
        erroRefresh(tokenRequest("refresh",p.refresh()));
        novaTransacao(() -> {
            assertEquals(1,sessoes(f).size());
            assertEquals(motivo,sessoes(f).getFirst().getMotivoRevogacao());
            assertFalse(sessoes(f).getFirst().foiUtilizada());
            return null;
        });
    }

    @ParameterizedTest @ValueSource(strings={"login","refresh","logout"})
    void deveRetornar400ParaJsonMalformado(String rota) throws Exception {
        var r=requisicao(post("/api/autenticacao/"+rota).contentType(MediaType.APPLICATION_JSON).content("{"),400);
        assertEquals("JSON inválido ou malformado",JsonPath.read(r.getResponse().getContentAsString(),"$.message"));
    }

    @Test void logoutIndividualDeveAfetarSomenteUmaFamiliaSemInvalidarJwt() throws Exception {
        var propria=fixture(); var outra=fixture();
        var r1=login(propria); var r2=renovar(r1);
        var segundaFamilia=login(propria); var outraConta=login(outra);
        logout(r1.refresh()); // Mesmo uma geração utilizada identifica a família correta.
        logout(r2.refresh()); // Idempotência, preserva o primeiro motivo.
        logout("desconhecido");
        novaTransacao(() -> {
            var lista=sessoes(propria);
            assertEquals(3,lista.size());
            assertTrue(lista.subList(0,2).stream().allMatch(s -> s.foiRevogada() && s.getMotivoRevogacao()==MotivoRevogacaoRefreshToken.LOGOUT));
            assertFalse(lista.getLast().foiRevogada());
            assertFalse(sessoes(outra).getFirst().foiRevogada());
            return null;
        });
        erroRefresh(tokenRequest("refresh",r2.refresh()));
        renovar(segundaFamilia); renovar(outraConta);
        consultarComBearer(r1,propria);
        assertTrue(Instant.now().isBefore(r1.expira()));
    }

    @Test void logoutGlobalDeveExigirBearerERevogarSomentePropriaConta() throws Exception {
        var propria=fixture(); var outra=fixture();
        var p1=login(propria); var p2=login(propria); var outraSessao=login(outra);
        for(var request:List.of(post("/api/autenticacao/logout-todos"),
                post("/api/autenticacao/logout-todos").header("Authorization","Bearer invalido"))) {
            var r=requisicao(request,401);
            assertTrue(r.getResponse().getContentAsString().isEmpty());
            assertTrue(r.getResponse().getHeader("WWW-Authenticate").startsWith("Bearer"));
        }
        novaTransacao(() -> {assertTrue(sessoes(propria).stream().noneMatch(SessaoRefreshToken::foiRevogada));return null;});
        var resultado=requisicao(post("/api/autenticacao/logout-todos").header("Authorization","Bearer "+p1.access())
                .contentType(MediaType.APPLICATION_JSON).content("{\"contaId\":"+outra.contaId()+",\"login\":\""+outra.login()+"\"}"),204);
        assertTrue(resultado.getResponse().getContentAsString().isEmpty());
        novaTransacao(() -> {
            var lista=sessoes(propria);
            assertEquals(2,lista.size());
            assertTrue(lista.stream().allMatch(s -> s.foiRevogada() && s.getMotivoRevogacao()==MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL));
            assertFalse(sessoes(outra).getFirst().foiRevogada());
            return null;
        });
        erroRefresh(tokenRequest("refresh",p1.refresh())); erroRefresh(tokenRequest("refresh",p2.refresh()));
        renovar(outraSessao);
        consultarComBearer(p1,propria);
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void logoutGlobalDeveRevalidarContaEPessoa(boolean pessoaInativa) throws Exception {
        var f=fixture(); var p=login(f);
        novaTransacao(() -> {
            var conta=em.find(ContaAcesso.class,f.contaId());
            if(pessoaInativa) conta.getPessoa().inativar(); else conta.bloquear();
            return null;
        });
        var r=requisicao(post("/api/autenticacao/logout-todos").header("Authorization","Bearer "+p.access()),401);
        assertEquals("Login ou senha inválidos",JsonPath.read(r.getResponse().getContentAsString(),"$.message"));
        assertNull(r.getResponse().getHeader("WWW-Authenticate"));
        novaTransacao(() -> {assertTrue(sessoes(f).stream().noneMatch(SessaoRefreshToken::foiRevogada));return null;});
        erroRefresh(tokenRequest("refresh",p.refresh()));
        novaTransacao(() -> {
            assertEquals(MotivoRevogacaoRefreshToken.CONTA_INATIVA,sessoes(f).getFirst().getMotivoRevogacao());
            return null;
        });
    }
}
