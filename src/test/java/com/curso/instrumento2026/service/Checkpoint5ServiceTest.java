package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.exception.*;
import com.curso.instrumento2026.repository.CategoriaInstrumentoRepository;
import com.curso.instrumento2026.repository.InstrumentoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Checkpoint5ServiceTest {
    @Autowired private CategoriaInstrumentoService categorias;
    @Autowired private InstrumentoService instrumentos;
    @Autowired private CategoriaInstrumentoRepository categoriaRepository;
    @Autowired private InstrumentoRepository instrumentoRepository;
    @Autowired private EntityManager em;

    @Test
    void deveNormalizarNomeEAceitarOProprioNomePorDirtyChecking() {
        var categoria = categoria("CP5 nome");
        categorias.alterar(categoria.getId(), "  CP5 NOME  ");
        em.flush();
        em.clear();
        assertEquals("CP5 NOME", categorias.buscarPorId(categoria.getId()).getNome());
    }

    @Test
    void deveRejeitarNomeDuplicadoIgnorandoCaixaEEspacos() {
        categoria("CP5 existente");
        var outra = categoria("CP5 outra");
        assertThrows(RecursoDuplicadoException.class,
                () -> categorias.alterar(outra.getId(), "  CP5 EXISTENTE  "));
        assertEquals("CP5 outra", outra.getNome());
    }

    @Test
    void deveRejeitarNomeEmBranco() {
        var categoria = categoria("CP5 valida");
        assertThrows(IllegalArgumentException.class,
                () -> categorias.alterar(categoria.getId(), "  "));
        assertEquals("CP5 valida", categoria.getNome());
    }

    @Test
    void devePersistirStatusDeCategoriaEInstrumento() {
        var categoria = categoria("CP5 status");
        var instrumento = instrumento(categoria);
        categorias.alterarStatus(categoria.getId(), Status.INATIVO);
        instrumentos.alterarStatus(instrumento.getId(), Status.INATIVO);
        em.flush();
        em.clear();
        assertEquals(Status.INATIVO, categorias.buscarPorId(categoria.getId()).getStatus());
        assertEquals(Status.INATIVO, instrumentos.buscarPorId(instrumento.getId()).getStatus());
        categorias.alterarStatus(categoria.getId(), Status.ATIVO);
        instrumentos.alterarStatus(instrumento.getId(), Status.ATIVO);
        em.flush();
        em.clear();
        assertEquals(Status.ATIVO, categorias.buscarPorId(categoria.getId()).getStatus());
        assertEquals(Status.ATIVO, instrumentos.buscarPorId(instrumento.getId()).getStatus());
    }

    @Test
    void deveImpedirExclusaoDeCategoriaEmUso() {
        var categoria = categoria("CP5 em uso");
        instrumento(categoria);
        assertThrows(RecursoEmUsoException.class, () -> categorias.excluir(categoria.getId()));
        assertTrue(categoriaRepository.existsById(categoria.getId()));
    }

    @Test
    void deveExcluirInstrumentoEDepoisCategoria() {
        var categoria = categoria("CP5 excluir");
        var instrumento = instrumento(categoria);
        instrumentos.excluir(instrumento.getId());
        assertFalse(instrumentoRepository.existsById(instrumento.getId()));
        categorias.excluir(categoria.getId());
        assertFalse(categoriaRepository.existsById(categoria.getId()));
    }

    @Test
    void deveAlterarDadosAssociacoesERemoverFornecedorPorDirtyChecking() {
        var antiga = categoria("CP5 antiga");
        var nova = categoria("CP5 nova");
        var instrumento = instrumento(antiga);
        var fornecedor = new Fornecedor("Fornecedor CP5", "78912345000166");
        em.persist(fornecedor);
        instrumentos.alterar(instrumento.getId(), "Nome inicial", new BigDecimal("150.00"),
                BigDecimal.ONE, antiga.getId(), fornecedor.getId());
        assertSame(fornecedor, instrumento.getFornecedor());
        instrumentos.alterar(instrumento.getId(), "Nome alterado", new BigDecimal("200.00"),
                new BigDecimal("3.000"), nova.getId(), null);
        assertFalse(antiga.getInstrumentos().contains(instrumento));
        assertTrue(nova.getInstrumentos().contains(instrumento));
        em.flush();
        em.clear();
        var salvo = instrumentos.buscarPorId(instrumento.getId());
        assertEquals("Nome alterado", salvo.getNomeInstrumento());
        assertEquals(new BigDecimal("200.00"), salvo.getPrecoUnitario());
        assertEquals(new BigDecimal("3.000"), salvo.getEstoqueMinimo());
        assertEquals(nova.getId(), salvo.getCategoria().getId());
        assertNull(salvo.getFornecedor());
        assertEquals("CP5-INSTRUMENTO", salvo.getCodigoInstrumento());
        assertEquals(5, salvo.getQuantidadeEstoque());
        assertEquals(Status.ATIVO, salvo.getStatus());
        assertEquals(LocalDate.of(2026, 1, 1), salvo.getDataCadastro());
    }

    @Test
    void deveResolverRelacionamentosAntesDeAlterarDados() {
        var categoria = categoria("CP5 referencia");
        var instrumento = instrumento(categoria);
        assertThrows(RecursoNaoEncontradoException.class,
                () -> instrumentos.alterar(instrumento.getId(), "Nao aplicar",
                        BigDecimal.TEN, BigDecimal.ONE, categoria.getId(), Long.MAX_VALUE));
        assertEquals("Instrumento CP5", instrumento.getNomeInstrumento());
        assertSame(categoria, instrumento.getCategoria());
    }

    @Test
    void deveRegistrarEntradaESaidaPersistindoSaldo() {
        var instrumento = instrumento(categoria("CP5 estoque"));
        instrumentos.receberEstoque(instrumento.getId(), new BigDecimal("3.000"));
        assertEquals(8, instrumento.getQuantidadeEstoque());
        instrumentos.retirarEstoque(instrumento.getId(), new BigDecimal("2.000"));
        em.flush();
        em.clear();
        assertEquals(6, instrumentos.buscarPorId(instrumento.getId()).getQuantidadeEstoque());
    }

    @Test
    void deveRejeitarSaidaInsuficienteSemAlterarSaldo() {
        var instrumento = instrumento(categoria("CP5 insuficiente"));
        assertThrows(IllegalArgumentException.class,
                () -> instrumentos.retirarEstoque(instrumento.getId(), BigDecimal.TEN));
        em.clear();
        assertEquals(5, instrumentos.buscarPorId(instrumento.getId()).getQuantidadeEstoque());
    }

    @Test
    void deveRejeitarMovimentacoesInvalidasSemTruncarQuantidade() {
        var instrumento = instrumento(categoria("CP5 fracao"));
        assertThrows(IllegalArgumentException.class,
                () -> instrumentos.receberEstoque(instrumento.getId(), new BigDecimal("1.5")));
        assertThrows(IllegalArgumentException.class,
                () -> instrumentos.retirarEstoque(instrumento.getId(), new BigDecimal("1.5")));
        assertThrows(IllegalArgumentException.class,
                () -> instrumentos.receberEstoque(instrumento.getId(), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> instrumentos.retirarEstoque(instrumento.getId(), BigDecimal.ONE.negate()));
        assertThrows(IllegalArgumentException.class,
                () -> instrumentos.receberEstoque(instrumento.getId(), null));
        assertThrows(IllegalArgumentException.class,
                () -> instrumentos.receberEstoque(instrumento.getId(), new BigDecimal("2147483648")));
        assertEquals(5, instrumento.getQuantidadeEstoque());
    }

    @Test
    void deveRejeitarOperacoesComIdsInexistentes() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> categorias.alterar(Long.MAX_VALUE, "CP5 ausente"));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> categorias.alterarStatus(Long.MAX_VALUE, Status.ATIVO));
        assertThrows(RecursoNaoEncontradoException.class, () -> categorias.excluir(Long.MAX_VALUE));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> instrumentos.alterar(Long.MAX_VALUE, "Ausente", BigDecimal.TEN,
                        BigDecimal.ZERO, Long.MAX_VALUE, null));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> instrumentos.alterarStatus(Long.MAX_VALUE, Status.ATIVO));
        assertThrows(RecursoNaoEncontradoException.class, () -> instrumentos.excluir(Long.MAX_VALUE));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> instrumentos.receberEstoque(Long.MAX_VALUE, BigDecimal.ONE));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> instrumentos.retirarEstoque(Long.MAX_VALUE, BigDecimal.ONE));
    }

    private CategoriaInstrumento categoria(String nome) {
        return categorias.cadastrar(new CategoriaInstrumento(null, nome));
    }

    private Instrumento instrumento(CategoriaInstrumento categoria) {
        return instrumentos.cadastrar(new Instrumento("CP5-INSTRUMENTO", "Instrumento CP5", 5,
                new BigDecimal("100.00"), BigDecimal.ZERO, LocalDate.of(2026, 1, 1)),
                categoria.getId(), null);
    }
}
