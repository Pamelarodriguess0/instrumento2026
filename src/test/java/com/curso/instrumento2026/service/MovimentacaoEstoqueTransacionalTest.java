package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.domain.Status;
import com.curso.instrumento2026.repository.CategoriaInstrumentoRepository;
import com.curso.instrumento2026.repository.InstrumentoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// Sem transação no teste: as verificações finais observam commits reais.
@SpringBootTest
@ActiveProfiles("test")
class MovimentacaoEstoqueTransacionalTest {
    @Autowired InstrumentoService service;
    @Autowired InstrumentoRepository instrumentos;
    @Autowired CategoriaInstrumentoRepository categorias;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    private Long instrumentoId;
    private Long categoriaId;

    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            String codigo = UUID.randomUUID().toString();
            var categoria = categorias.save(new CategoriaInstrumento(null, "Movimentação " + codigo));
            categoriaId = categoria.getId();
            var instrumento = new Instrumento(codigo, "Violino", 5, new BigDecimal("100.00"),
                    BigDecimal.ZERO, LocalDate.now());
            instrumento.definirCategoria(categoria);
            instrumentoId = instrumentos.save(instrumento).getId();
        });
    }

    @AfterEach
    void limpar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            instrumentos.deleteById(instrumentoId);
            instrumentos.flush();
            categorias.deleteById(categoriaId);
        });
    }

    @Test
    void devePreservarEstoqueConfirmadoAoAlterarCadastroComSaldoAntigo() throws Exception {
        verificarAlteracaoComSaldoAntigo(false);
    }

    @Test
    void devePreservarEstoqueConfirmadoAoAlterarStatusComSaldoAntigo() throws Exception {
        verificarAlteracaoComSaldoAntigo(true);
    }

    private void verificarAlteracaoComSaldoAntigo(boolean alterarStatus) throws Exception {
        CountDownLatch cadastroCarregado = new CountDownLatch(1);
        CountDownLatch estoqueConfirmado = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var cadastro = executor.submit(() -> {
                var transacao = transacaoLimitada();
                return transacao.execute(status -> {
                    configurarTimeoutsSql();
                    int sessao = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                    Instrumento antigo = instrumentos.findById(instrumentoId).orElseThrow();
                    assertEquals(5, antigo.getQuantidadeEstoque());
                    cadastroCarregado.countDown();
                    aguardar(estoqueConfirmado);
                    // A movimentação já confirmou saldo 8, mas este contexto ainda contém saldo 5.
                    assertEquals(5, antigo.getQuantidadeEstoque());
                    Instrumento alterado = alterarStatus
                            ? service.alterarStatus(instrumentoId, Status.INATIVO)
                            : service.alterar(instrumentoId, "Violino cadastral", new BigDecimal("175.00"),
                                    new BigDecimal("2.000"), categoriaId, null);
                    assertSame(antigo, alterado);
                    assertEquals(5, alterado.getQuantidadeEstoque());
                    return sessao;
                });
            });
            var movimentacao = executor.submit(() -> {
                aguardar(cadastroCarregado);
                int sessao = transacaoLimitada().execute(status -> {
                    configurarTimeoutsSql();
                    int conexao = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                    service.receberEstoque(instrumentoId, new BigDecimal("3"));
                    return conexao;
                });
                // O callback já terminou e a movimentação foi efetivamente commitada.
                estoqueConfirmado.countDown();
                return sessao;
            });
            int sessaoMovimentacao = movimentacao.get(15, TimeUnit.SECONDS);
            int sessaoCadastro = cadastro.get(15, TimeUnit.SECONDS);
            assertNotEquals(sessaoCadastro, sessaoMovimentacao);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }

        Instrumento salvo = instrumentos.findById(instrumentoId).orElseThrow();
        assertEquals(8, salvo.getQuantidadeEstoque());
        assertEquals(alterarStatus ? Status.INATIVO : Status.ATIVO, salvo.getStatus());
        assertEquals(alterarStatus ? "Violino" : "Violino cadastral", salvo.getNomeInstrumento());
        assertEquals(new BigDecimal(alterarStatus ? "100.00" : "175.00"), salvo.getPrecoUnitario());
        assertEquals(new BigDecimal(alterarStatus ? "0.000" : "2.000"), salvo.getEstoqueMinimo());
        assertEquals(categoriaId, salvo.getCategoria().getId());
        assertNull(salvo.getFornecedor());
        System.out.printf("CADASTRO CONCORRENTE: operacao=%s, estoque=%d, nome=%s, preco=%s, status=%s%n",
                alterarStatus ? "status" : "cadastro", salvo.getQuantidadeEstoque(),
                salvo.getNomeInstrumento(), salvo.getPrecoUnitario(), salvo.getStatus());
    }

    private TransactionTemplate transacaoLimitada() {
        var transacao = new TransactionTemplate(transactionManager);
        transacao.setTimeout(12);
        return transacao;
    }

    private void configurarTimeoutsSql() {
        jdbc.execute("SET LOCAL lock_timeout = '5s'");
        jdbc.execute("SET LOCAL statement_timeout = '8s'");
    }

    private void aguardar(CountDownLatch sinal) {
        try {
            assertTrue(sinal.await(10, TimeUnit.SECONDS), "Sinal concorrente não recebido");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void devePreservarMovimentacoesNomeEPrecoSemFlushManualAposCommit() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Instrumento instrumento = instrumentos.findById(instrumentoId).orElseThrow();
            instrumento.alterarNome("Violino revisado");
            instrumento.alterarPrecoUnitario(new BigDecimal("150.00"));
            service.receberEstoque(instrumentoId, new BigDecimal("3"));
            assertEquals(8, instrumento.getQuantidadeEstoque());
            assertEquals("Violino revisado", instrumento.getNomeInstrumento());
            instrumento.alterarNome("Violino final");
            instrumento.alterarPrecoUnitario(new BigDecimal("200.00"));
            service.retirarEstoque(instrumentoId, new BigDecimal("2"));
            assertEquals(6, instrumento.getQuantidadeEstoque());
        });

        Instrumento salvo = instrumentos.findById(instrumentoId).orElseThrow();
        assertEquals(6, salvo.getQuantidadeEstoque());
        assertEquals("Violino final", salvo.getNomeInstrumento());
        assertEquals(new BigDecimal("200.00"), salvo.getPrecoUnitario());
    }

    @Test
    void deveCoordenarSaidasConcorrentesMesmoComOutrosAtributosPendentes() throws Exception {
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        List<String> resultados;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> retirarComAlteracoesPendentes(prontas, inicio));
            var segunda = executor.submit(() -> retirarComAlteracoesPendentes(prontas, inicio));
            try {
                assertTrue(prontas.await(10, TimeUnit.SECONDS));
            } finally {
                inicio.countDown();
            }
            resultados = List.of(primeira.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS));
        }
        assertEquals(1, resultados.stream().filter("sucesso"::equals).count());
        assertEquals(1, resultados.stream().filter("Estoque insuficiente"::equals).count());
        Instrumento salvo = instrumentos.findById(instrumentoId).orElseThrow();
        assertEquals(1, salvo.getQuantidadeEstoque());
        assertEquals("Violino concorrente", salvo.getNomeInstrumento());
        assertEquals(new BigDecimal("250.00"), salvo.getPrecoUnitario());
    }

    private String retirarComAlteracoesPendentes(CountDownLatch prontas, CountDownLatch inicio) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                Instrumento instrumento = instrumentos.findById(instrumentoId).orElseThrow();
                assertEquals(5, instrumento.getQuantidadeEstoque());
                instrumento.alterarNome("Violino concorrente");
                instrumento.alterarPrecoUnitario(new BigDecimal("250.00"));
                prontas.countDown();
                try {
                    assertTrue(inicio.await(10, TimeUnit.SECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                service.retirarEstoque(instrumentoId, new BigDecimal("4"));
            });
            return "sucesso";
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        }
    }
}
