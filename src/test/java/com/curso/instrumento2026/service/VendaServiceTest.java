package com.curso.instrumento2026.service;

import org.springframework.security.test.context.support.WithMockUser;

import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

// Sem @Transactional no teste: cada chamada de service confirma ou reverte sua própria transação.
@WithMockUser(roles = "ADMINISTRADOR")
@SpringBootTest
@ActiveProfiles("test")
class VendaServiceTest {
    @Autowired VendaService service;
    @Autowired PessoaRepository pessoas;
    @Autowired ClienteRepository clientes;
    @Autowired ColaboradorRepository colaboradores;
    @Autowired InstrumentoRepository instrumentos;
    @Autowired CategoriaInstrumentoRepository categorias;
    @Autowired VendaRepository vendas;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private TransactionTemplate tx;
    private Long pessoaClienteId, pessoaVendedorId, clienteId, vendedorId, primeiroId, segundoId, categoriaId;

    @BeforeEach
    void prepararEmTransacaoConfirmada() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            var pessoaCliente = pessoas.save(new Pessoa("Cliente venda service", "cliente.venda@example.com", "52998224725", LocalDate.now()));
            var pessoaVendedor = pessoas.save(new Pessoa("Vendedor venda service", "vendedor.venda@example.com", "11144477735", LocalDate.now()));
            pessoaClienteId = pessoaCliente.getId();
            pessoaVendedorId = pessoaVendedor.getId();
            clienteId = clientes.save(new Cliente(pessoaCliente, null)).getId();
            vendedorId = colaboradores.save(new Colaborador(pessoaVendedor, "VEN-SERVICE", FuncaoColaborador.VENDEDOR, LocalDate.now())).getId();
            var categoria = categorias.save(new CategoriaInstrumento(null, "Categoria venda service"));
            categoriaId = categoria.getId();
            primeiroId = instrumento("VEN-SERVICE-A", 5, categoria).getId();
            segundoId = instrumento("VEN-SERVICE-B", 1, categoria).getId();
        });
    }

    private Instrumento instrumento(String codigo, int saldo, CategoriaInstrumento categoria) {
        var instrumento = new Instrumento(codigo, "Violino", saldo, new BigDecimal("100.00"), BigDecimal.ZERO, LocalDate.now());
        instrumento.definirCategoria(categoria);
        return instrumentos.save(instrumento);
    }

    @AfterEach
    void removerSomenteDadosCriadosPeloTeste() {
        tx.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM venda WHERE cliente_id = ?", clienteId);
            jdbc.update("DELETE FROM colaborador WHERE id = ?", vendedorId);
            jdbc.update("DELETE FROM cliente WHERE id = ?", clienteId);
            jdbc.update("DELETE FROM pessoa WHERE id IN (?,?)", pessoaClienteId, pessoaVendedorId);
            jdbc.update("DELETE FROM instrumento WHERE id IN (?,?)", primeiroId, segundoId);
            jdbc.update("DELETE FROM categoria_instrumento WHERE id = ?", categoriaId);
        });
    }

    private Long abrir() { return service.cadastrar(clienteId, vendedorId).getId(); }
    private int saldo(Long id) { return instrumentos.findById(id).orElseThrow().getQuantidadeEstoque(); }

    @Test
    void deveFinalizarMesmaVendaApenasUmaVezSobConcorrencia() throws Exception {
        tx.executeWithoutResult(status -> instrumentos.findById(primeiroId).orElseThrow().adicionarEstoque(5));
        Long vendaId = abrir();
        service.adicionarItem(vendaId, primeiroId, new BigDecimal("4"));
        assertEquals(10, saldo(primeiroId));

        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        Set<Integer> sessoes = ConcurrentHashMap.newKeySet();
        List<String> resultados;
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primeira = executor.submit(() -> finalizarMesmaVenda(vendaId, prontas, inicio, sessoes));
            var segunda = executor.submit(() -> finalizarMesmaVenda(vendaId, prontas, inicio, sessoes));
            try {
                assertTrue(prontas.await(10, TimeUnit.SECONDS), "Ambas devem carregar a mesma venda ABERTA");
                assertEquals(2, sessoes.size(), "São necessárias duas sessões PostgreSQL independentes");
            } finally {
                inicio.countDown();
            }
            resultados = List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS));
        } finally {
            inicio.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS), "As tarefas devem encerrar antes da limpeza");
        }

        long aceitas = resultados.stream().filter("sucesso"::equals).count();
        int saldoFinal = saldo(primeiroId);
        StatusVenda estadoFinal = service.buscarPorId(vendaId).getStatus();
        System.out.printf("MESMA VENDA: sessoes=%d, aceitas=%d, resultados=%s, estoque=%d, status=%s%n",
                sessoes.size(), aceitas, resultados, saldoFinal, estadoFinal);
        // Contrato esperado: teste de regressão fica vermelho enquanto a duplicidade existir.
        assertAll(
                () -> assertEquals(1, aceitas, "Somente uma finalização deve ser aceita"),
                () -> assertEquals(6, saldoFinal, "Somente quatro unidades devem ser retiradas"),
                () -> assertEquals(StatusVenda.FINALIZADA, estadoFinal),
                () -> assertEquals(1, resultados.stream()
                        .filter("Somente vendas abertas podem ser alteradas"::equals).count(),
                        "A segunda tentativa deve falhar pelo estado da venda"));
    }

    private String finalizarMesmaVenda(Long vendaId, CountDownLatch prontas, CountDownLatch inicio,
                                      Set<Integer> sessoes) {
        try {
            var transacao = new TransactionTemplate(transactionManager);
            transacao.setTimeout(10);
            transacao.executeWithoutResult(status -> {
                jdbc.execute("SET LOCAL lock_timeout = '5s'");
                jdbc.execute("SET LOCAL statement_timeout = '8s'");
                sessoes.add(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                Venda venda = vendas.findOneById(vendaId).orElseThrow();
                assertEquals(StatusVenda.ABERTA, venda.getStatus());
                assertEquals(10, venda.getItens().getFirst().getInstrumento().getQuantidadeEstoque());
                prontas.countDown();
                try {
                    assertTrue(inicio.await(10, TimeUnit.SECONDS), "Sinal de início não recebido");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                service.finalizar(vendaId);
            });
            return "sucesso";
        } catch (IllegalStateException exception) {
            return exception.getMessage();
        }
    }

    @Test
    void deveImpedirVendaAcimaDoSaldoEmFinalizacoesConcorrentes() throws Exception {
        Long primeira = abrir();
        Long segunda = abrir();
        service.adicionarItem(primeira, primeiroId, new BigDecimal("4"));
        service.adicionarItem(segunda, primeiroId, new BigDecimal("4"));
        assertEquals(5, saldo(primeiroId));

        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        List<ResultadoFinalizacao> resultados;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeiro = executor.submit(() -> finalizarConcorrentemente(primeira, prontas, inicio));
            var segundo = executor.submit(() -> finalizarConcorrentemente(segunda, prontas, inicio));
            try {
                assertTrue(prontas.await(10, TimeUnit.SECONDS), "As duas transações devem carregar o saldo inicial");
            } finally {
                inicio.countDown();
            }
            resultados = List.of(primeiro.get(10, TimeUnit.SECONDS), segundo.get(10, TimeUnit.SECONDS));
        }

        assertEquals(1, resultados.stream().filter(ResultadoFinalizacao::sucesso).count());
        assertEquals(1, resultados.stream().filter(resultado -> !resultado.sucesso()).count());
        // Consultas após o término das duas transações, sem reutilizar suas entidades gerenciadas.
        for (ResultadoFinalizacao resultado : resultados) {
            assertEquals(resultado.sucesso() ? StatusVenda.FINALIZADA : StatusVenda.ABERTA,
                    service.buscarPorId(resultado.vendaId()).getStatus());
            if (!resultado.sucesso()) assertEquals("Estoque insuficiente", resultado.mensagem());
        }
        // Somente as quatro unidades da vencedora foram baixadas; a perdedora não consumiu estoque.
        assertEquals(1, saldo(primeiroId));
    }

    private ResultadoFinalizacao finalizarConcorrentemente(Long vendaId, CountDownLatch prontas,
                                                           CountDownLatch inicio) {
        try {
            // Cada thread tem sua própria transação/contexto; o service participa dela pelo proxy Spring.
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                Venda venda = vendas.findOneById(vendaId).orElseThrow();
                assertEquals(5, venda.getItens().getFirst().getInstrumento().getQuantidadeEstoque());
                prontas.countDown();
                try {
                    assertTrue(inicio.await(10, TimeUnit.SECONDS), "Sinal de início não recebido");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Finalização concorrente interrompida", exception);
                }
                service.finalizar(vendaId);
            });
            return new ResultadoFinalizacao(vendaId, true, null);
        } catch (IllegalArgumentException exception) {
            // A exceção atravessou o limite transacional: o rollback já ocorreu.
            return new ResultadoFinalizacao(vendaId, false, exception.getMessage());
        }
    }

    private record ResultadoFinalizacao(Long vendaId, boolean sucesso, String mensagem) {}

    @Test
    void deveFinalizarBaixandoCadaItemUmaVezEPreservandoPrecoHistorico() {
        Long id = abrir();
        service.adicionarItem(id, primeiroId, new BigDecimal("2.000"));
        service.adicionarItem(id, segundoId, BigDecimal.ONE);
        tx.executeWithoutResult(status -> instrumentos.findById(primeiroId).orElseThrow().alterarPrecoUnitario(new BigDecimal("999.00")));
        assertEquals(5, saldo(primeiroId));
        service.finalizar(id);
        assertEquals(3, saldo(primeiroId));
        assertEquals(0, saldo(segundoId));
        assertEquals(StatusVenda.FINALIZADA, service.buscarPorId(id).getStatus());
        assertEquals(new BigDecimal("300.00"), service.buscarPorId(id).calcularTotal());
        assertThrows(IllegalStateException.class, () -> service.finalizar(id));
        assertEquals(3, saldo(primeiroId));
        assertEquals(0, saldo(segundoId));
    }

    @Test
    void deveFazerRollbackDaPrimeiraBaixaSeSegundoItemNaoTemSaldo() {
        Long id = abrir();
        service.adicionarItem(id, primeiroId, new BigDecimal("2"));
        service.adicionarItem(id, segundoId, new BigDecimal("2"));
        assertEquals("Estoque insuficiente", assertThrows(IllegalArgumentException.class, () -> service.finalizar(id)).getMessage());
        // Releituras fora da transação que falhou, consultando o estado confirmado no banco.
        assertEquals(5, saldo(primeiroId));
        assertEquals(1, saldo(segundoId));
        assertEquals(StatusVenda.ABERTA, service.buscarPorId(id).getStatus());
    }

    @Test
    void deveFazerRollbackSeSegundoInstrumentoFoiInativado() {
        Long id = abrir();
        service.adicionarItem(id, primeiroId, BigDecimal.ONE);
        service.adicionarItem(id, segundoId, BigDecimal.ONE);
        tx.executeWithoutResult(status -> instrumentos.findById(segundoId).orElseThrow().inativar());
        assertThrows(IllegalArgumentException.class, () -> service.finalizar(id));
        assertEquals(5, saldo(primeiroId));
        assertEquals(StatusVenda.ABERTA, service.buscarPorId(id).getStatus());
    }

    @Test
    void deveValidarPessoasNaAberturaERevalidarNaFinalizacao() {
        tx.executeWithoutResult(status -> pessoas.findById(pessoaClienteId).orElseThrow().inativar());
        assertThrows(IllegalArgumentException.class, this::abrir);
        tx.executeWithoutResult(status -> pessoas.findById(pessoaClienteId).orElseThrow().ativar());
        Long id = abrir();
        service.adicionarItem(id, primeiroId, BigDecimal.ONE);
        tx.executeWithoutResult(status -> pessoas.findById(pessoaVendedorId).orElseThrow().inativar());
        assertThrows(IllegalArgumentException.class, () -> service.finalizar(id));
        assertThrows(IllegalArgumentException.class, this::abrir);
        assertEquals(5, saldo(primeiroId));
        assertEquals(StatusVenda.ABERTA, service.buscarPorId(id).getStatus());
    }

    @ParameterizedTest
    @EnumSource(FuncaoColaborador.class)
    void deveAceitarSomenteVendedorOuGerente(FuncaoColaborador funcao) {
        tx.executeWithoutResult(status -> colaboradores.findById(vendedorId).orElseThrow().alterarDados("VEN-SERVICE", funcao));
        if (funcao == FuncaoColaborador.GERENTE || funcao == FuncaoColaborador.VENDEDOR) {
            Long id = abrir();
            service.adicionarItem(id, primeiroId, BigDecimal.ONE);
            assertEquals(StatusVenda.FINALIZADA, service.finalizar(id).getStatus());
        } else {
            assertThrows(IllegalArgumentException.class, this::abrir);
        }
    }

    @Test
    void deveRevalidarFuncaoAlteradaAposAbertura() {
        Long id = abrir();
        service.adicionarItem(id, primeiroId, BigDecimal.ONE);
        tx.executeWithoutResult(status -> colaboradores.findById(vendedorId).orElseThrow().alterarDados("VEN-SERVICE", FuncaoColaborador.ESTOQUISTA));
        assertThrows(IllegalArgumentException.class, () -> service.finalizar(id));
        assertEquals(5, saldo(primeiroId));
    }

    @Test
    void devePersistirRemocaoDeItemSemMovimentarEstoque() {
        Long id = abrir();
        var item = service.adicionarItem(id, primeiroId, BigDecimal.ONE).getItens().getFirst();
        assertNotNull(item.getId());
        service.removerItem(id, item.getId());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM item_venda WHERE id = ?", Integer.class, item.getId()));
        assertEquals(5, saldo(primeiroId));
        assertThrows(IllegalArgumentException.class, () -> service.removerItem(id, item.getId()));
    }

    @Test
    void deveRejeitarVendaVaziaEItemInvalidoSemEfeitosParciais() {
        Long id = abrir();
        assertThrows(IllegalArgumentException.class, () -> service.finalizar(id));
        assertThrows(IllegalArgumentException.class, () -> service.adicionarItem(id, primeiroId, new BigDecimal("1.5")));
        assertThrows(IllegalArgumentException.class, () -> service.adicionarItem(id, primeiroId, BigDecimal.ZERO));
        tx.executeWithoutResult(status -> instrumentos.findById(segundoId).orElseThrow().inativar());
        assertThrows(IllegalArgumentException.class, () -> service.adicionarItem(id, segundoId, BigDecimal.ONE));
        assertTrue(service.buscarPorId(id).getItens().isEmpty());
        service.adicionarItem(id, primeiroId, BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> service.adicionarItem(id, primeiroId, BigDecimal.ONE));
        assertEquals(1, service.buscarPorId(id).getItens().size());
    }

    @Test
    void deveCancelarSemBaixarEstoqueEBloquearAlteracoesPosteriores() {
        Long id = abrir();
        var item = service.adicionarItem(id, primeiroId, BigDecimal.ONE).getItens().getFirst();
        service.cancelar(id);
        assertEquals(StatusVenda.CANCELADA, service.buscarPorId(id).getStatus());
        assertEquals(5, saldo(primeiroId));
        assertThrows(IllegalStateException.class, () -> service.adicionarItem(id, segundoId, BigDecimal.ONE));
        assertThrows(IllegalStateException.class, () -> service.removerItem(id, item.getId()));
        assertThrows(IllegalStateException.class, () -> service.finalizar(id));
        assertThrows(IllegalStateException.class, () -> service.cancelar(id));
    }

    @Test
    void deveTratarIdsInexistentes() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarPorId(Long.MAX_VALUE));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.cadastrar(Long.MAX_VALUE, vendedorId));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.cadastrar(clienteId, Long.MAX_VALUE));
        Long id = abrir();
        assertThrows(RecursoNaoEncontradoException.class, () -> service.adicionarItem(id, Long.MAX_VALUE, BigDecimal.ONE));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.adicionarItem(Long.MAX_VALUE, primeiroId, BigDecimal.ONE));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.removerItem(Long.MAX_VALUE, 1L));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.finalizar(Long.MAX_VALUE));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.cancelar(Long.MAX_VALUE));
    }

    @Test
    void devePesquisarComDatasInclusivasFiltrosEPaginacao() {
        Long primeiro = abrir();
        Long segundo = abrir();
        Long fora = abrir();
        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE venda SET data_venda = ? WHERE id = ?", LocalDateTime.of(2026, 10, 22, 0, 0), primeiro);
            jdbc.update("UPDATE venda SET data_venda = ? WHERE id = ?", LocalDateTime.of(2026, 10, 22, 23, 59, 59, 999999000), segundo);
            jdbc.update("UPDATE venda SET data_venda = ? WHERE id = ?", LocalDateTime.of(2026, 10, 23, 0, 0), fora);
        });
        var pagina = service.pesquisar(StatusVenda.ABERTA, clienteId, vendedorId,
                LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 22), PageRequest.of(0, 1, Sort.by("dataVenda")));
        assertEquals(2, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(primeiro, pagina.getContent().getFirst().getId());
        assertEquals(0, service.pesquisar(StatusVenda.CANCELADA, clienteId, null, null, null, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, service.pesquisar(null, null, Long.MAX_VALUE, null, null, PageRequest.of(0, 10)).getTotalElements());
        assertThrows(IllegalArgumentException.class, () -> service.pesquisar(null, null, null,
                LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 22), PageRequest.of(0, 10)));
        assertThrows(IllegalArgumentException.class, () -> service.pesquisar(null, null, null,
                null, null, PageRequest.of(0, 10, Sort.by("cliente.pessoa.nome"))));
    }
}
