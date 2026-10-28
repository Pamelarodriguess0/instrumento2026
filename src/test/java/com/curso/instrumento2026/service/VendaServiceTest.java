package com.curso.instrumento2026.service;

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
import static org.junit.jupiter.api.Assertions.*;

// Sem @Transactional no teste: cada chamada de service confirma ou reverte sua própria transação.
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
