package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.Pessoa;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ContaAcessoServiceConcorrenciaTest {
    private static final String SENHA = "Frase de Teste Concorrente 2026";
    @Autowired ContaAcessoRepository contas;
    @Autowired PessoaRepository pessoas;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private Long pessoaId;
    private Long outraPessoaId;

    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            String execucao = UUID.randomUUID().toString();
            pessoaId = pessoas.save(new Pessoa("Concorrencia conta A", execucao + "@example.com",
                    "52998224725", LocalDate.now())).getId();
            outraPessoaId = pessoas.save(new Pessoa("Concorrencia conta B", execucao + ".b@example.com",
                    "11144477735", LocalDate.now())).getId();
        });
    }

    @AfterEach
    void limpar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM conta_acesso WHERE pessoa_id IN (?,?)", pessoaId, outraPessoaId);
            jdbc.update("DELETE FROM pessoa WHERE id IN (?,?)", pessoaId, outraPessoaId);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveArbitrarUnicidadeNoBancoAposAmbasPreConsultas(boolean mesmaPessoa) throws Exception {
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        // A barreira fica no encoder, depois das pré-consultas reais e antes da gravação.
        PasswordEncoder coordenado = new PasswordEncoder() {
            @Override
            public String encode(CharSequence senha) {
                String hash = encoder.encode(senha);
                prontas.countDown();
                try {
                    assertTrue(inicio.await(10, TimeUnit.SECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                return hash;
            }

            @Override
            public boolean matches(CharSequence senha, String hash) {
                return encoder.matches(senha, hash);
            }
        };
        // Usa o mesmo serviço e os repositories reais; cada tarefa fornece sua transação explicitamente.
        var service = new ContaAcessoService(contas, pessoas, coordenado);
        String login = "concorrente." + UUID.randomUUID();
        List<ResultadoCadastro> resultados;
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primeiro = executor.submit(() -> cadastrar(service, pessoaId, mesmaPessoa ? login + ".a" : login));
            var segundo = executor.submit(() -> cadastrar(service, mesmaPessoa ? pessoaId : outraPessoaId,
                    mesmaPessoa ? login + ".b" : login));
            try {
                assertTrue(prontas.await(10, TimeUnit.SECONDS), "Ambos devem ultrapassar as pré-consultas");
            } finally {
                inicio.countDown();
            }
            resultados = List.of(primeiro.get(20, TimeUnit.SECONDS), segundo.get(20, TimeUnit.SECONDS));
        } finally {
            inicio.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
        assertEquals(1, resultados.stream().filter(ResultadoCadastro::sucesso).count());
        var falhas = resultados.stream().filter(resultado -> !resultado.sucesso()).toList();
        assertEquals(1, falhas.size());
        assertEquals(mesmaPessoa ? "Pessoa já possui conta de acesso" : "Login já cadastrado", falhas.getFirst().mensagem());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM conta_acesso WHERE pessoa_id IN (?,?)",
                Integer.class, pessoaId, outraPessoaId));
        String hash = jdbc.queryForObject("SELECT senha_hash FROM conta_acesso WHERE pessoa_id IN (?,?)",
                String.class, pessoaId, outraPessoaId);
        assertTrue(hash.startsWith("{bcrypt}"));
        assertTrue(encoder.matches(SENHA, hash));
    }

    private ResultadoCadastro cadastrar(ContaAcessoService service, Long pessoa, String login) {
        try {
            var transacao = new TransactionTemplate(transactionManager);
            transacao.setTimeout(15);
            transacao.executeWithoutResult(status -> {
                jdbc.execute("SET LOCAL lock_timeout='5s'");
                jdbc.execute("SET LOCAL statement_timeout='8s'");
                service.cadastrar(pessoa, login, SENHA);
            });
            return new ResultadoCadastro(true, null);
        } catch (RecursoDuplicadoException exception) {
            assertNull(exception.getCause());
            return new ResultadoCadastro(false, exception.getMessage());
        }
    }

    private record ResultadoCadastro(boolean sucesso, String mensagem) {}
}
