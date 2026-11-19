package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.support.ProtecaoAutenticacaoPersistenciaSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** Sem transação externa: threads, commits e leitura final usam transações independentes. */
class ProtecaoAutenticacaoConcorrenciaTest extends ProtecaoAutenticacaoPersistenciaSupport {
    @Autowired ProtecaoAutenticacaoRepository repository;
    @Autowired PlatformTransactionManager manager;
    private String chave;

    @BeforeEach
    void prepararChaveExclusiva() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        chave = novaChave();
    }

    @AfterEach
    void limparSomenteProtecaoDesteTeste() {
        if (chave != null) novaTransacao(() -> jdbc.update(
                "delete from protecao_autenticacao where chave_login=?", chave));
    }

    private <T> T novaTransacao(Supplier<T> acao) {
        var tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setTimeout(20);
        return tx.execute(status -> {
            jdbc.execute("set local lock_timeout = '12s'");
            return acao.get();
        });
    }

    private void aguardar(CountDownLatch sinal) {
        try {
            if (!sinal.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Coordenação excedeu prazo");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Coordenação interrompida", exception);
        }
    }

    private void confirmarEsperaNoPostgreSQL(int primeiraPid, int segundaPid) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        var intervalo = new CountDownLatch(1);
        boolean bloqueada = false;
        while (System.nanoTime() < limite) {
            bloqueada = Boolean.TRUE.equals(jdbc.queryForObject("""
                    select wait_event_type='Lock' and ? = any(pg_blocking_pids(pid))
                    from pg_stat_activity where pid=?
                    """, Boolean.class, primeiraPid, segundaPid));
            if (bloqueada) break;
            intervalo.await(25, TimeUnit.MILLISECONDS);
        }
        assertTrue(bloqueada, "A segunda transação deve aguardar a primeira no PostgreSQL");
    }

    @Test
    void upsertsConcorrentesDevemCriarSomenteUmaProtecao() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var primeiraInseriu = new CountDownLatch(1);
        var segundaIniciou = new CountDownLatch(1);
        var liberarPrimeira = new CountDownLatch(1);
        var pidA = new AtomicInteger();
        var pidB = new AtomicInteger();
        try {
            var a = pool.submit(() -> novaTransacao(() -> {
                pidA.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                int criadas = repository.inserirSeAusente(chave);
                primeiraInseriu.countDown();
                aguardar(liberarPrimeira);
                return criadas;
            }));
            aguardar(primeiraInseriu);
            var b = pool.submit(() -> novaTransacao(() -> {
                pidB.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                segundaIniciou.countDown();
                return repository.inserirSeAusente(chave);
            }));
            aguardar(segundaIniciou);
            assertNotEquals(pidA.get(), pidB.get());
            confirmarEsperaNoPostgreSQL(pidA.get(), pidB.get());
            liberarPrimeira.countDown();
            assertEquals(1, a.get(20, TimeUnit.SECONDS));
            assertEquals(0, b.get(20, TimeUnit.SECONDS));
            novaTransacao(() -> {
                assertEquals(1, jdbc.queryForObject("select count(*) from protecao_autenticacao where chave_login=?",
                        Integer.class, chave));
                assertEquals(0, repository.buscarParaAtualizacao(chave).orElseThrow().getFalhasConsecutivas());
                return null;
            });
        } finally {
            liberarPrimeira.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));
        }
    }

    @Test
    void lockDevePreservarDuasFalhasConcorrentesDeTresParaCinco() throws Exception {
        novaTransacao(() -> {
            repository.inserirSeAusente(chave);
            var protecao = repository.buscarParaAtualizacao(chave).orElseThrow();
            for (int i = 0; i < 3; i++) protecao.registrarFalha(INICIO, 5, PRAZO, PRAZO);
            return null;
        });
        var pool = Executors.newFixedThreadPool(2);
        var primeiraComLock = new CountDownLatch(1);
        var segundaIniciou = new CountDownLatch(1);
        var liberarPrimeira = new CountDownLatch(1);
        var pidA = new AtomicInteger();
        var pidB = new AtomicInteger();
        try {
            var a = pool.submit(() -> novaTransacao(() -> {
                pidA.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                var protecao = repository.buscarParaAtualizacao(chave).orElseThrow();
                boolean ativou = protecao.registrarFalha(INICIO, 5, PRAZO, PRAZO);
                em.flush();
                primeiraComLock.countDown();
                aguardar(liberarPrimeira);
                return ativou;
            }));
            aguardar(primeiraComLock);
            var b = pool.submit(() -> novaTransacao(() -> {
                pidB.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                segundaIniciou.countDown();
                return repository.buscarParaAtualizacao(chave).orElseThrow()
                        .registrarFalha(INICIO, 5, PRAZO, PRAZO);
            }));
            aguardar(segundaIniciou);
            assertNotEquals(pidA.get(), pidB.get());
            confirmarEsperaNoPostgreSQL(pidA.get(), pidB.get());
            liberarPrimeira.countDown();
            assertFalse(a.get(20, TimeUnit.SECONDS));
            assertTrue(b.get(20, TimeUnit.SECONDS));
            novaTransacao(() -> {
                var protecao = repository.buscarParaAtualizacao(chave).orElseThrow();
                assertEquals(5, protecao.getFalhasConsecutivas());
                assertEquals(INICIO, protecao.getJanelaIniciadaEm());
                assertEquals(INICIO.plus(PRAZO), protecao.getBloqueadaAte());
                return null;
            });
        } finally {
            liberarPrimeira.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));
        }
    }
}
