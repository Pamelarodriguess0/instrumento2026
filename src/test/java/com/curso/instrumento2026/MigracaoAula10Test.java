package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MigracaoAula10Test {
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    private Pessoa pessoa;
    private Cliente cliente;
    private Colaborador colaborador;
    private Instrumento instrumento;
    private Venda venda;
    private ItemVenda item;

    @BeforeEach
    void preparar() {
        pessoa = new Pessoa("Pessoa migracao", "migracao@example.com", "52998224725", LocalDate.now());
        em.persist(pessoa);
        cliente = new Cliente(pessoa, null);
        colaborador = new Colaborador(pessoa, "MIG-001", FuncaoColaborador.VENDEDOR, LocalDate.now());
        em.persist(cliente);
        em.persist(colaborador);
        var categoria = new CategoriaInstrumento(null, "Categoria migracao 005");
        em.persist(categoria);
        instrumento = new Instrumento("MIG-005", "Violino migracao", 5, new BigDecimal("100.00"),
                BigDecimal.ZERO, LocalDate.now());
        instrumento.definirCategoria(categoria);
        em.persist(instrumento);
        venda = new Venda(cliente, colaborador, LocalDateTime.now());
        item = venda.adicionarItem(instrumento, new BigDecimal("2.000"));
        em.persist(venda);
        em.flush();
    }

    @Test
    void deveRegistrar21ChangeSetsNovosSemReaplicarOs18Anteriores() {
        assertEquals(21, jdbc.queryForObject("SELECT COUNT(*) FROM databasechangelog "
                + "WHERE filename = 'db/changelog/changes/005-pessoas-clientes-vendas.yaml'", Integer.class));
        assertEquals(18, jdbc.queryForObject("SELECT COUNT(*) FROM databasechangelog "
                + "WHERE filename IN ('db/changelog/changes/001-create-categoria-instrumento.yaml',"
                + "'db/changelog/changes/002-create-instrumento.yaml',"
                + "'db/changelog/changes/003-fornecedor-e-estoque-minimo.yaml',"
                + "'db/changelog/changes/004-unique-nome-categoria-instrumento.yaml')", Integer.class));
    }

    @Test
    void devePersistirERelerPapeisVendaEPrecoHistorico() {
        instrumento.alterarPrecoUnitario(new BigDecimal("150.00"));
        em.flush();
        em.clear();
        var salva = em.find(Venda.class, venda.getId());
        assertEquals(pessoa.getId(), salva.getCliente().getPessoa().getId());
        assertEquals(pessoa.getId(), salva.getVendedor().getPessoa().getId());
        assertEquals(new BigDecimal("200.00"), salva.calcularTotal());
        assertEquals(new BigDecimal("100.00"), salva.getItens().getFirst().getValorUnitario());
        assertEquals(instrumento.getId(), salva.getItens().getFirst().getInstrumento().getId());
    }

    @Test
    void deveImpedirCpfDuplicadoNoBanco() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO pessoa(nome,email,cpf,data_cadastro,status) VALUES (?,?,?,CURRENT_DATE,'ATIVO')",
                "Outra", "outra@example.com", pessoa.getCpf()));
    }

    @Test
    void deveImpedirEmailDuplicadoIgnorandoCaixaEEspacos() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO pessoa(nome,email,cpf,data_cadastro,status) VALUES (?,?,?,CURRENT_DATE,'ATIVO')",
                "Outra", " MIGRACAO@EXAMPLE.COM ", "11144477735"));
    }

    @Test
    void deveImpedirRepetirPapelDeCliente() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO cliente(pessoa_id) VALUES (?)", pessoa.getId()));
    }

    @Test
    void deveImpedirRepetirPapelDeColaborador() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO colaborador(pessoa_id,matricula,funcao,data_admissao) VALUES (?,?,'GERENTE',CURRENT_DATE)",
                pessoa.getId(), "MIG-002"));
    }

    @Test
    void deveImpedirRepetirMatricula() {
        var outra = new Pessoa("Outra", "outra@example.com", "11144477735", LocalDate.now());
        em.persist(outra);
        em.flush();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO colaborador(pessoa_id,matricula,funcao,data_admissao) VALUES (?,?,'GERENTE',CURRENT_DATE)",
                outra.getId(), colaborador.getMatricula()));
    }

    @Test
    void deveImpedirRepetirInstrumentoNaVenda() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO item_venda(venda_id,instrumento_id,quantidade,valor_unitario) VALUES (?,?,1,10)",
                venda.getId(), instrumento.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "2147483648"})
    void deveRejeitarQuantidadeIncompativelComEstoqueInteiro(String quantidade) {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE item_venda SET quantidade = ? WHERE id = ?", new BigDecimal(quantidade), item.getId()));
    }

    @Test
    void deveRejeitarPrecoNegativo() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE item_venda SET valor_unitario = -1 WHERE id = ?", item.getId()));
    }

    @Test
    void deveImpedirExcluirInstrumentoVinculado() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("DELETE FROM instrumento WHERE id = ?", instrumento.getId()));
    }

    @Test
    void deveExcluirItensEmCascataSemExcluirParticipantesOuInstrumento() {
        jdbc.update("DELETE FROM venda WHERE id = ?", venda.getId());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM item_venda WHERE venda_id = ?", Integer.class, venda.getId()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM instrumento WHERE id = ?", Integer.class, instrumento.getId()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM cliente WHERE id = ?", Integer.class, cliente.getId()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM colaborador WHERE id = ?", Integer.class, colaborador.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "UPDATE pessoa SET status = 'INVALIDO'",
            "UPDATE pessoa SET cpf = '123'",
            "UPDATE colaborador SET funcao = 'INVALIDA'",
            "UPDATE venda SET status = 'INVALIDO'"
    })
    void deveAplicarChecksDeFormatoEEnumeracoes(String sql) {
        Long id = sql.startsWith("UPDATE pessoa") ? pessoa.getId()
                : sql.startsWith("UPDATE colaborador") ? colaborador.getId() : venda.getId();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql + " WHERE id = ?", id));
    }
}
