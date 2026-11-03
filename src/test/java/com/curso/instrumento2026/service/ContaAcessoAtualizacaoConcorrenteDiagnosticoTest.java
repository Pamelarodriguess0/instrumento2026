package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.Pessoa;
import com.curso.instrumento2026.domain.StatusContaAcesso;
import com.curso.instrumento2026.repository.ContaAcessoRepository;
import com.curso.instrumento2026.repository.PessoaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// Sem transação externa: duas transações sobrepostas leem antes do primeiro commit.
// As expectativas de preservação de senha/status permanecem vermelhas se houver perda.
@SpringBootTest
@ActiveProfiles("test")
class ContaAcessoAtualizacaoConcorrenteDiagnosticoTest {
    private static final String ANTIGA = "Frase anterior diagnostico 2026";
    private static final String NOVA = "Frase nova diagnostico seguro 2026";
    @Autowired ContaAcessoService service;
    @Autowired ContaAcessoRepository contas;
    @Autowired PessoaRepository pessoas;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private Long pessoaId;
    private Long contaId;
    private String login;

    @BeforeEach
    void preparar() {
        String token = UUID.randomUUID().toString();
        login = "concorrencia.atualizacao." + token;
        pessoaId = transacao().execute(status -> pessoas.save(new Pessoa(
                "Diagnostico atualizacao conta", token + "@example.com", "52998224725", LocalDate.now())).getId());
        contaId = service.cadastrar(pessoaId, login, ANTIGA).getId();
    }

    @AfterEach
    void limpar() {
        transacao().executeWithoutResult(status -> {
            jdbc.update("DELETE FROM conta_acesso WHERE pessoa_id=?", pessoaId);
            jdbc.update("DELETE FROM pessoa WHERE id=?", pessoaId);
        });
    }

    private TransactionTemplate transacao() {
        var tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.setTimeout(20);
        return tx;
    }

    @ParameterizedTest(name = "Preservar senha e bloqueio; troca confirma primeiro={0}")
    @ValueSource(booleans = {true, false})
    void devePreservarSenhaNovaEBloqueioEmTransacoesConcorrentes(boolean senhaPrimeiro) throws Exception {
        CountDownLatch carregadas = new CountDownLatch(2);
        CountDownLatch primeiroCommit = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        Sessao senha;
        Sessao bloqueio;
        try {
            var troca = executor.submit(() -> executar(true, senhaPrimeiro, carregadas, primeiroCommit));
            var bloquear = executor.submit(() -> executar(false, senhaPrimeiro, carregadas, primeiroCommit));
            senha = troca.get(30, TimeUnit.SECONDS);
            bloqueio = bloquear.get(30, TimeUnit.SECONDS);
        } finally {
            primeiroCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS), "As tarefas devem terminar antes da limpeza");
        }
        assertNotNull(senha);
        assertNotNull(bloqueio);
        assertNotEquals(senha.backendPid(), bloqueio.backendPid(), "Conexões PostgreSQL independentes");
        assertNotEquals(senha.transactionId(), bloqueio.transactionId(), "Transações PostgreSQL independentes");
        // Outra transação/contexto após o retorno dos dois commits, sem reutilizar as entidades antigas.
        var finalPersistido = transacao().execute(status -> jdbc.queryForObject(
                "SELECT status,senha_hash FROM conta_acesso WHERE id=?",
                (rs, row) -> new EstadoPersistido(rs.getString("status"), rs.getString("senha_hash")), contaId));
        assertNotNull(finalPersistido);
        boolean aceitaAntiga = encoder.matches(ANTIGA, finalPersistido.hash());
        boolean aceitaNova = encoder.matches(NOVA, finalPersistido.hash());
        String ordem = senhaPrimeiro ? "senha -> bloqueio" : "bloqueio -> senha";
        System.out.printf("DIAGNOSTICO CONCORRENCIA: ordem=%s, sessoes=2, transacoes=2, "
                        + "status=%s, aceitaAntiga=%s, aceitaNova=%s%n",
                ordem, finalPersistido.status(), aceitaAntiga, aceitaNova);
        assertAll("Ambas as alterações devem sobreviver aos commits: " + ordem,
                () -> assertEquals("BLOQUEADA", finalPersistido.status(), "Troca de senha não deve desfazer bloqueio"),
                () -> assertTrue(aceitaNova, "Bloqueio não deve restaurar hash anterior"),
                () -> assertFalse(aceitaAntiga, "Senha anterior deve permanecer inválida"));
    }

    private Sessao executar(boolean trocaSenha, boolean senhaPrimeiro, CountDownLatch carregadas,
                             CountDownLatch primeiroCommit) {
        boolean primeira = trocaSenha == senhaPrimeiro;
        try {
            return transacao().execute(status -> {
                jdbc.execute("SET LOCAL lock_timeout='5s'");
                jdbc.execute("SET LOCAL statement_timeout='8s'");
                Long pid = jdbc.queryForObject("SELECT pg_backend_pid()", Long.class);
                Long xid = jdbc.queryForObject("SELECT txid_current()", Long.class);
                // Prepara um contexto gerenciado antigo em cada transação, um fluxo JPA permitido.
                // O serviço real participa da transação da tarefa e reencontra essa mesma entidade.
                var contaCarregada = contas.findOneById(contaId).orElseThrow();
                assertEquals(StatusContaAcesso.ATIVA, contaCarregada.getStatus());
                assertTrue(encoder.matches(ANTIGA, contaCarregada.getSenhaHash()));
                carregadas.countDown();
                aguardar(carregadas, "Ambas as transações devem carregar o estado inicial");
                if (!primeira) aguardar(primeiroCommit, "A primeira alteração deve confirmar antes da segunda");
                if (trocaSenha) service.alterarSenha(login, ANTIGA, NOVA);
                else service.alterarStatus(contaId, StatusContaAcesso.BLOQUEADA);
                return new Sessao(pid, xid);
            });
        } finally {
            // execute só retorna após commit/rollback: não libera a segunda tarefa antes disso.
            if (primeira) primeiroCommit.countDown();
        }
    }

    private static void aguardar(CountDownLatch latch, String mensagem) {
        try {
            assertTrue(latch.await(12, TimeUnit.SECONDS), mensagem);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Diagnóstico interrompido", exception);
        }
    }

    private record Sessao(Long backendPid, Long transactionId) {}
    private record EstadoPersistido(String status, String hash) {}
}
