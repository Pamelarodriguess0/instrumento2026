package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoriesPasso6Test {
    @Autowired PessoaRepository pessoas;
    @Autowired ClienteRepository clientes;
    @Autowired ColaboradorRepository colaboradores;
    @Autowired VendaRepository vendas;
    @Autowired EntityManager em;
    private Pessoa ana;
    private Pessoa bruno;
    private Cliente cliente;
    private Colaborador vendedor;
    private Colaborador outroColaborador;
    private Venda venda;

    @BeforeEach
    void preparar() {
        ana = pessoas.save(new Pessoa("Ana Passo6", "ana.passo6@example.com", "52998224725", LocalDate.now()));
        bruno = pessoas.save(new Pessoa("Bruno Passo6", "bruno.passo6@example.com", "11144477735", LocalDate.now()));
        cliente = clientes.save(new Cliente(ana, null));
        clientes.save(new Cliente(bruno, "11999990000"));
        vendedor = colaboradores.save(new Colaborador(bruno, "PASSO6-01", FuncaoColaborador.VENDEDOR, LocalDate.now()));
        outroColaborador = colaboradores.save(new Colaborador(ana, "PASSO6-02", FuncaoColaborador.GERENTE, LocalDate.now()));
        var categoria = new CategoriaInstrumento(null, "Categoria Passo6");
        em.persist(categoria);
        var primeiro = instrumento("PASSO6-A", categoria);
        var segundo = instrumento("PASSO6-B", categoria);
        venda = new Venda(cliente, vendedor, LocalDateTime.now());
        venda.adicionarItem(primeiro, new BigDecimal("2.000"));
        venda.adicionarItem(segundo, BigDecimal.ONE);
        vendas.save(venda);
        var outra = new Venda(cliente, vendedor, LocalDateTime.now());
        outra.adicionarItem(primeiro, BigDecimal.ONE);
        outra.cancelar();
        vendas.saveAndFlush(outra);
        em.clear();
    }

    private Instrumento instrumento(String codigo, CategoriaInstrumento categoria) {
        var instrumento = new Instrumento(codigo, "Violino " + codigo, 5, new BigDecimal("100.00"),
                BigDecimal.ZERO, LocalDate.now());
        instrumento.definirCategoria(categoria);
        em.persist(instrumento);
        return instrumento;
    }

    @Test
    void deveVerificarCpfEEmailExcluindoOProprioId() {
        assertTrue(pessoas.existsByCpf(ana.getCpf()));
        assertFalse(pessoas.existsByCpf("01234567890"));
        assertFalse(pessoas.existsByCpfAndIdNot(ana.getCpf(), ana.getId()));
        assertTrue(pessoas.existsByCpfAndIdNot(ana.getCpf(), bruno.getId()));
        assertFalse(pessoas.existsByCpfAndIdNot("01234567890", ana.getId()));
        assertTrue(pessoas.existsByEmailIgnoreCase("ANA.PASSO6@EXAMPLE.COM"));
        assertFalse(pessoas.existsByEmailIgnoreCase("ausente@example.com"));
        assertFalse(pessoas.existsByEmailIgnoreCaseAndIdNot("ANA.PASSO6@EXAMPLE.COM", ana.getId()));
        assertTrue(pessoas.existsByEmailIgnoreCaseAndIdNot("ANA.PASSO6@EXAMPLE.COM", bruno.getId()));
        assertFalse(pessoas.existsByEmailIgnoreCaseAndIdNot("ausente@example.com", ana.getId()));
    }

    @Test
    void deveVerificarPapeisEMatricula() {
        assertTrue(clientes.existsByPessoaId(ana.getId()));
        assertTrue(colaboradores.existsByPessoaId(bruno.getId()));
        assertFalse(clientes.existsByPessoaId(Long.MAX_VALUE));
        assertFalse(colaboradores.existsByPessoaId(Long.MAX_VALUE));
        assertTrue(colaboradores.existsByMatricula("PASSO6-01"));
        assertFalse(colaboradores.existsByMatricula("AUSENTE"));
        assertFalse(colaboradores.existsByMatriculaAndIdNot("PASSO6-01", vendedor.getId()));
        assertTrue(colaboradores.existsByMatriculaAndIdNot("PASSO6-01", outroColaborador.getId()));
        assertFalse(colaboradores.existsByMatriculaAndIdNot("AUSENTE", vendedor.getId()));
    }

    @Test
    void deveCarregarPessoaNoDetalheDeClienteEColaboradorAposDetach() {
        var salvo = clientes.findOneById(cliente.getId()).orElseThrow();
        assertTrue(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(salvo, "pessoa"));
        em.clear();
        assertEquals("Ana Passo6", salvo.getPessoa().getNome());
        var colaborador = colaboradores.findOneById(vendedor.getId()).orElseThrow();
        assertTrue(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(colaborador, "pessoa"));
        em.clear();
        assertEquals("Bruno Passo6", colaborador.getPessoa().getNome());
    }

    @Test
    void devePaginarPapeisComPessoaCarregadaEAplicarSpecification() {
        Specification<Cliente> filtroCliente = (root, query, cb) -> cb.equal(root.get("pessoa").get("id"), ana.getId());
        var paginaCliente = clientes.findAll(filtroCliente, PageRequest.of(0, 1));
        assertEquals(1, paginaCliente.getTotalElements());
        assertTrue(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(paginaCliente.getContent().getFirst(), "pessoa"));
        em.clear();
        assertEquals("Ana Passo6", paginaCliente.getContent().getFirst().getPessoa().getNome());
        Specification<Colaborador> filtroColaborador = (root, query, cb) -> cb.equal(root.get("funcao"), FuncaoColaborador.VENDEDOR);
        var paginaColaborador = colaboradores.findAll(filtroColaborador, PageRequest.of(0, 1));
        assertEquals(1, paginaColaborador.getTotalElements());
        em.clear();
        assertEquals("Bruno Passo6", paginaColaborador.getContent().getFirst().getPessoa().getNome());
    }

    @Test
    void deveCarregarDetalheDaVendaComParticipantesPessoasEInstrumentos() {
        var salva = vendas.findOneById(venda.getId()).orElseThrow();
        var util = em.getEntityManagerFactory().getPersistenceUnitUtil();
        assertTrue(util.isLoaded(salva, "itens"));
        assertEquals(2, salva.getItens().size());
        salva.getItens().forEach(item -> assertTrue(util.isLoaded(item, "instrumento")));
        em.clear();
        assertEquals("Ana Passo6", salva.getCliente().getPessoa().getNome());
        assertEquals("Bruno Passo6", salva.getVendedor().getPessoa().getNome());
        assertTrue(salva.getItens().stream().allMatch(item -> item.getInstrumento().getNomeInstrumento().startsWith("Violino")));
        assertEquals(new BigDecimal("300.00"), salva.calcularTotal());
    }

    @Test
    void devePaginarVendasSemCarregarItensNemDistorcerContagem() {
        Specification<Venda> filtro = (root, query, cb) -> cb.equal(root.get("cliente").get("id"), cliente.getId());
        var pagina = vendas.findAll(filtro, PageRequest.of(0, 1, Sort.by("id")));
        assertEquals(2, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(1, pagina.getContent().size());
        var resumo = pagina.getContent().getFirst();
        assertFalse(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(resumo, "itens"));
        em.clear();
        assertEquals("Ana Passo6", resumo.getCliente().getPessoa().getNome());
        assertEquals("Bruno Passo6", resumo.getVendedor().getPessoa().getNome());
        var segunda = vendas.findAll(filtro, PageRequest.of(1, 1, Sort.by("id")));
        assertNotEquals(resumo.getId(), segunda.getContent().getFirst().getId());
        assertEquals(2, segunda.getTotalElements());
    }

    @Test
    void devePermitirSpecificationDePessoaEVendaEIdsAusentes() {
        Specification<Pessoa> filtroPessoa = (root, query, cb) -> cb.equal(root.get("cpf"), ana.getCpf());
        assertEquals(1, pessoas.findAll(filtroPessoa, PageRequest.of(0, 1)).getTotalElements());
        Specification<Venda> filtroVenda = (root, query, cb) -> cb.equal(root.get("status"), StatusVenda.CANCELADA);
        assertEquals(1, vendas.findAll(filtroVenda, PageRequest.of(0, 1)).getTotalElements());
        assertTrue(clientes.findOneById(Long.MAX_VALUE).isEmpty());
        assertTrue(colaboradores.findOneById(Long.MAX_VALUE).isEmpty());
        assertTrue(vendas.findOneById(Long.MAX_VALUE).isEmpty());
    }
}
