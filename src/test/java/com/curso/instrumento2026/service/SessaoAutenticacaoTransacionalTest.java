package com.curso.instrumento2026.service;

import com.curso.instrumento2026.config.JwtProperties;
import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.exception.RefreshTokenInvalidoException;
import com.curso.instrumento2026.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Sem transação de teste: cada chamada ao proxy confirma ou desfaz sua própria transação. */
@SpringBootTest
@ActiveProfiles("test")
class SessaoAutenticacaoTransacionalTest {
    private static final Instant INICIO = Instant.parse("2026-11-17T18:00:00Z");
    @Autowired SessaoAutenticacaoService service;
    @Autowired AutenticacaoService loginService;
    @Autowired ContaAcessoRepository contas;
    @Autowired SessaoRefreshTokenRepository sessoes;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtProperties jwtProperties;
    @Autowired org.springframework.core.env.Environment environment;
    @MockitoBean Clock clock;
    @MockitoSpyBean TokenService tokens;
    private Long contaId;
    private Long pessoaId;
    private String login;
    private TransactionTemplate tx;

    @BeforeEach void preparar() {
        assertTrue(Arrays.asList(environment.getActiveProfiles()).contains("test"));
        String destino = "jdbc:postgresql://127.0.0.1:5432/instrumento2026_test";
        assertEquals(destino, environment.getProperty("spring.datasource.url"));
        assertEquals(destino, environment.getProperty("spring.liquibase.url"));
        assertEquals("public", environment.getProperty("spring.datasource.hikari.schema"));
        assertEquals("public", environment.getProperty("spring.liquibase.default-schema"));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        when(clock.instant()).thenReturn(INICIO);
        tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setTimeout(20);
        login = "ponto.c." + UUID.randomUUID();
        novaTransacao(() -> {
            var pessoa = new Pessoa("Pessoa Ponto C", login + "@example.com", "52998224725", LocalDate.of(2026,11,17));
            em.persist(pessoa);
            var conta = new ContaAcesso(pessoa, login, passwords.encode("senha-sintetica-teste"));
            em.persist(conta);
            contaId = conta.getId();
            pessoaId = pessoa.getId();
            return null;
        });
    }

    @AfterEach void limpar() {
        // Limpeza restrita aos IDs desta fixture, em transação confirmada.
        if (contaId != null) novaTransacao(() -> {
            var conta = em.find(ContaAcesso.class, contaId);
            if (conta != null) em.remove(conta); // FK CASCADE remove as sessões.
            em.flush();
            var pessoa = em.find(Pessoa.class, pessoaId);
            if (pessoa != null) em.remove(pessoa);
            return null;
        });
    }

    private <T> T novaTransacao(Supplier<T> acao) { return tx.execute(status -> acao.get()); }
    private TokensAutenticacao iniciar() {
        return service.iniciar(UsernamePasswordAuthenticationToken.authenticated(login, null, List.of()));
    }
    private List<SessaoRefreshToken> geracoes() {
        return em.createQuery("select s from SessaoRefreshToken s where s.conta.id=:conta order by s.id", SessaoRefreshToken.class)
                .setParameter("conta", contaId).getResultList();
    }
    private static String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    }
    private Jwt decodificar(TokenAcesso access) {
        var key = new SecretKeySpec(Base64.getDecoder().decode(jwtProperties.secret()), "HmacSHA256");
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var tempo = new JwtTimestampValidator();
        tempo.setClock(Clock.fixed(INICIO, ZoneOffset.UTC));
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(tempo, new JwtIssuerValidator(jwtProperties.issuer())));
        return decoder.decode(access.valor());
    }

    @Test void deveCriarPrimeiraSessaoNoLoginReal() {
        var access = loginService.autenticar(login, "senha-sintetica-teste");
        var jwt = decodificar(access);
        novaTransacao(() -> {
            var lista = geracoes();
            assertEquals(1, lista.size());
            assertTrue(lista.getFirst().getFamiliaId().equals(jwt.getClaimAsString("sid")));
            assertFalse(lista.getFirst().foiUtilizada());
            assertFalse(lista.getFirst().foiRevogada());
            return null;
        });
    }

    @Test void devePersistirSomenteHashERotacionarComMesmoLimite() throws Exception {
        var r1 = iniciar();
        String esperado = hash(r1.refreshToken());
        novaTransacao(() -> {
            var s = geracoes().getFirst();
            assertTrue(esperado.equals(s.getTokenHash()), "Hash deve corresponder sem expor valores");
            assertFalse(s.getTokenHash().equals(r1.refreshToken()));
            String registro = (String) em.createNativeQuery("select row_to_json(s)::text from sessao_refresh_token s where conta_acesso_id=:conta")
                    .setParameter("conta",contaId).getSingleResult();
            assertFalse(registro.contains(r1.refreshToken()), "Registro não deve conter o segredo original");
            return null;
        });
        when(clock.instant()).thenReturn(INICIO.plusSeconds(60));
        var r2 = service.renovar(r1.refreshToken());
        assertFalse(r1.refreshToken().equals(r2.refreshToken()));
        assertEquals(r1.refreshTokenExpiresAt(), r2.refreshTokenExpiresAt());
        novaTransacao(() -> {
            var lista = geracoes();
            assertEquals(2,lista.size());
            assertEquals(INICIO.plusSeconds(60),lista.getFirst().getUtilizadaEm());
            assertTrue(lista.getFirst().getFamiliaId().equals(lista.getLast().getFamiliaId()));
            assertEquals(lista.getFirst().getExpiraEm(),lista.getLast().getExpiraEm());
            assertFalse(lista.getLast().foiUtilizada());
            return null;
        });
    }

    @Test void deveConfirmarRevogacaoMesmoAposExcecaoDeReplay() {
        var r1 = iniciar();
        var r2 = service.renovar(r1.refreshToken());
        assertThrows(RefreshTokenInvalidoException.class, () -> service.renovar(r1.refreshToken()));
        novaTransacao(() -> {
            var lista=geracoes();
            assertEquals(2,lista.size());
            assertTrue(lista.stream().allMatch(s -> s.foiRevogada() && s.getMotivoRevogacao()==MotivoRevogacaoRefreshToken.REUTILIZACAO));
            return null;
        });
        assertThrows(RefreshTokenInvalidoException.class, () -> service.renovar(r2.refreshToken()));
    }

    @Test void deveDesfazerUsoSeAssinaturaFalharAposFlushReal() {
        var r1 = iniciar();
        doAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            em.flush(); // utilizada_em já foi enviada ao PostgreSQL antes da falha.
            throw new JwtEncodingException("Falha sintética de assinatura");
        }).when(tokens).emitir(any(),anyString());
        assertThrows(JwtEncodingException.class, () -> service.renovar(r1.refreshToken()));
        novaTransacao(() -> {
            var lista=geracoes();
            assertEquals(1,lista.size());
            assertFalse(lista.getFirst().foiUtilizada());
            assertFalse(lista.getFirst().foiRevogada());
            return null;
        });
        doCallRealMethod().when(tokens).emitir(any(),anyString());
        assertNotNull(service.renovar(r1.refreshToken()));
    }

    @Test void deveDesfazerSucessorInseridoSeConfirmacaoFalhar() {
        var r1=iniciar();
        var observouInsercao = new java.util.concurrent.atomic.AtomicBoolean();
        doAnswer(invocation -> {
            TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void beforeCommit(boolean readOnly) {
                            em.flush();
                            var lista=geracoes();
                            observouInsercao.set(lista.size()==2 && lista.getFirst().foiUtilizada());
                            throw new IllegalStateException("Falha sintética antes do commit");
                        }
                    });
            return invocation.callRealMethod();
        }).when(tokens).emitir(any(),anyString());
        assertThrows(IllegalStateException.class,()->service.renovar(r1.refreshToken()));
        assertTrue(observouInsercao.get(),"Sucessor e utilização devem ter sido enviados ao banco antes da falha");
        novaTransacao(() -> {
            var lista=geracoes();
            assertEquals(1,lista.size());
            assertFalse(lista.getFirst().foiUtilizada());
            assertFalse(lista.getFirst().foiRevogada());
            return null;
        });
    }

    @ParameterizedTest
    @EnumSource(value=MotivoRevogacaoRefreshToken.class,names={"INATIVIDADE","EXPIRACAO_ABSOLUTA","LOGOUT"})
    void deveRejeitarExpiradoOuRevogado(MotivoRevogacaoRefreshToken motivo) {
        var r1=iniciar();
        if(motivo==MotivoRevogacaoRefreshToken.LOGOUT) novaTransacao(() -> {
            geracoes().getFirst().revogar(INICIO,motivo); return null;
        });
        else when(clock.instant()).thenReturn(INICIO.plusSeconds(motivo==MotivoRevogacaoRefreshToken.INATIVIDADE?1800:28800));
        assertThrows(RefreshTokenInvalidoException.class,()->service.renovar(r1.refreshToken()));
        novaTransacao(() -> {
            assertEquals(1,geracoes().size());
            assertEquals(motivo,geracoes().getFirst().getMotivoRevogacao());
            assertFalse(geracoes().getFirst().foiUtilizada());
            return null;
        });
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void deveRevalidarContaEPessoaEmNovaTransacao(boolean inativarPessoa) {
        var r1=iniciar();
        novaTransacao(() -> {
            var conta=em.find(ContaAcesso.class,contaId);
            if(inativarPessoa) conta.getPessoa().inativar(); else conta.bloquear();
            return null;
        });
        assertThrows(RefreshTokenInvalidoException.class,()->service.renovar(r1.refreshToken()));
        novaTransacao(() -> {
            assertEquals(1,geracoes().size());
            assertEquals(MotivoRevogacaoRefreshToken.CONTA_INATIVA,geracoes().getFirst().getMotivoRevogacao());
            return null;
        });
    }

    @Test void deveEmitirJwtComPerfisAtualizados() {
        var r1=iniciar();
        assertEquals(List.of("OPERADOR"),decodificar(r1.accessToken()).getClaimAsStringList("perfis"));
        novaTransacao(() -> { em.find(ContaAcesso.class,contaId).substituirPerfis(Set.of(PerfilAcesso.ADMINISTRADOR)); return null; });
        var r2=service.renovar(r1.refreshToken());
        var jwt=decodificar(r2.accessToken());
        assertEquals(List.of("ADMINISTRADOR"),jwt.getClaimAsStringList("perfis"));
        assertTrue(jwt.getClaimAsString("sid").equals(decodificar(r1.accessToken()).getClaimAsString("sid")));
    }

    @Test void deveSerializarDuasRenovacoesConcorrentesDoMesmoToken() throws Exception {
        var r1=iniciar();
        var pool=Executors.newFixedThreadPool(2);
        var primeiraComLock=new CountDownLatch(1);
        var liberarPrimeira=new CountDownLatch(1);
        doAnswer(invocation -> {
            // Só a requisição vencedora chega à emissão; a segunda deve esperar no SELECT FOR UPDATE.
            primeiraComLock.countDown();
            if(!liberarPrimeira.await(20,TimeUnit.SECONDS)) throw new IllegalStateException("Espera controlada excedida");
            return invocation.callRealMethod();
        }).when(tokens).emitir(any(),anyString());
        Callable<Boolean> renovar=() -> {
            try { service.renovar(r1.refreshToken()); return true; }
            catch(RefreshTokenInvalidoException expected) { return false; }
        };
        try {
            var a=pool.submit(renovar);
            assertTrue(primeiraComLock.await(10,TimeUnit.SECONDS));
            var b=pool.submit(renovar);
            boolean observouBloqueio=false;
            long prazo=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
            var intervalo=new CountDownLatch(1);
            while(System.nanoTime()<prazo) {
                observouBloqueio=novaTransacao(() -> ((Number)em.createNativeQuery("""
                        select count(*) from pg_stat_activity
                        where datname=current_database() and usename=current_user
                        and pid<>pg_backend_pid() and wait_event_type='Lock'
                        and query like '%sessao_refresh_token%'
                        and cardinality(pg_blocking_pids(pid))>0
                        """).getSingleResult()).intValue()>0);
                if(observouBloqueio) break;
                intervalo.await(25,TimeUnit.MILLISECONDS);
            }
            assertTrue(observouBloqueio,"PostgreSQL deve mostrar a segunda renovação bloqueada pela primeira");
            liberarPrimeira.countDown();
            boolean primeira=a.get(30,TimeUnit.SECONDS),segunda=b.get(30,TimeUnit.SECONDS);
            assertTrue(primeira != segunda, "Exatamente uma renovação deve ter sucesso");
            novaTransacao(() -> {
                var lista=geracoes();
                assertEquals(2,lista.size(),"Somente um sucessor deve existir");
                assertTrue(lista.stream().allMatch(s -> s.foiRevogada() && s.getMotivoRevogacao()==MotivoRevogacaoRefreshToken.REUTILIZACAO));
                return null;
            });
        } finally {
            liberarPrimeira.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(20,TimeUnit.SECONDS));
        }
    }
}
