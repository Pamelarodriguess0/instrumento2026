package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.exception.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PessoasEPapeisServiceTest {
    @Autowired PessoaService pessoas;
    @Autowired ClienteService clientes;
    @Autowired ColaboradorService colaboradores;
    @Autowired EntityManager em;

    private Pessoa pessoa(String nome, String email, String cpf) {
        return pessoas.cadastrar(new Pessoa(nome, email, cpf, LocalDate.of(2026, 10, 22)));
    }

    @Test
    void deveCadastrarAlterarEAlternarStatusPorDirtyChecking() {
        var pessoa = pessoa("Ana", " ANA@EXAMPLE.COM ", "52998224725");
        assertNotNull(pessoa.getId());
        pessoas.alterar(pessoa.getId(), "Ana alterada", "ANA@EXAMPLE.COM", pessoa.getCpf());
        pessoas.alterarStatus(pessoa.getId(), Status.INATIVO);
        em.flush();
        em.clear();
        var salva = pessoas.buscarPorId(pessoa.getId());
        assertEquals("Ana alterada", salva.getNome());
        assertEquals("ana@example.com", salva.getEmail());
        assertEquals(Status.INATIVO, salva.getStatus());
        assertEquals(LocalDate.of(2026, 10, 22), salva.getDataCadastro());
        pessoas.alterarStatus(pessoa.getId(), Status.ATIVO);
        assertEquals(Status.ATIVO, salva.getStatus());
    }

    @Test
    void deveRejeitarCpfDuplicadoNoCadastro() {
        pessoa("Ana", "ana@example.com", "52998224725");
        assertThrows(RecursoDuplicadoException.class,
                () -> pessoa("Outra", "outra@example.com", "52998224725"));
    }

    @Test
    void deveRejeitarEmailDuplicadoNoCadastro() {
        pessoa("Ana", "ana@example.com", "52998224725");
        assertThrows(RecursoDuplicadoException.class,
                () -> pessoa("Outra", " ANA@EXAMPLE.COM ", "11144477735"));
    }

    @Test
    void deveRejeitarDuplicidadeNaAlteracaoPreservandoDados() {
        var ana = pessoa("Ana", "ana@example.com", "52998224725");
        var bruno = pessoa("Bruno", "bruno@example.com", "11144477735");
        assertThrows(RecursoDuplicadoException.class,
                () -> pessoas.alterar(bruno.getId(), "Outro", "outro@example.com", ana.getCpf()));
        assertThrows(RecursoDuplicadoException.class,
                () -> pessoas.alterar(bruno.getId(), "Outro", " ANA@EXAMPLE.COM ", bruno.getCpf()));
        assertEquals("Bruno", bruno.getNome());
        assertEquals("bruno@example.com", bruno.getEmail());
    }

    @Test
    void devePermitirDoisPapeisEAlterarSomenteSeusDadosEditaveis() {
        var pessoa = pessoa("Ana", "ana@example.com", "52998224725");
        var cliente = clientes.cadastrar(pessoa.getId(), "11999990000");
        var colaborador = colaboradores.cadastrar(pessoa.getId(), " VEN-01 ", FuncaoColaborador.VENDEDOR, LocalDate.of(2026, 1, 1));
        clientes.alterar(cliente.getId(), null);
        colaboradores.alterar(colaborador.getId(), " VEN-01 ", FuncaoColaborador.GERENTE);
        em.flush();
        em.clear();
        var salvo = colaboradores.buscarPorId(colaborador.getId());
        assertEquals("VEN-01", salvo.getMatricula());
        assertEquals(FuncaoColaborador.GERENTE, salvo.getFuncao());
        assertEquals(LocalDate.of(2026, 1, 1), salvo.getDataAdmissao());
        assertEquals(pessoa.getId(), salvo.getPessoa().getId());
        assertNull(clientes.buscarPorId(cliente.getId()).getTelefone());
    }

    @Test
    void deveImpedirCriarPapeisParaPessoaInativa() {
        var pessoa = pessoa("Ana", "ana@example.com", "52998224725");
        pessoas.alterarStatus(pessoa.getId(), Status.INATIVO);
        assertThrows(IllegalArgumentException.class, () -> clientes.cadastrar(pessoa.getId(), null));
        assertThrows(IllegalArgumentException.class,
                () -> colaboradores.cadastrar(pessoa.getId(), "VEN-01", FuncaoColaborador.VENDEDOR, LocalDate.now()));
    }

    @Test
    void deveImpedirRepetirCadaPapel() {
        var pessoa = pessoa("Ana", "ana@example.com", "52998224725");
        clientes.cadastrar(pessoa.getId(), null);
        colaboradores.cadastrar(pessoa.getId(), "VEN-01", FuncaoColaborador.VENDEDOR, LocalDate.now());
        assertThrows(RecursoDuplicadoException.class, () -> clientes.cadastrar(pessoa.getId(), "1111111111"));
        assertThrows(RecursoDuplicadoException.class,
                () -> colaboradores.cadastrar(pessoa.getId(), "VEN-02", FuncaoColaborador.GERENTE, LocalDate.now()));
    }

    @Test
    void deveRejeitarMatriculaDuplicadaNoCadastroEAlteracao() {
        var ana = pessoa("Ana", "ana@example.com", "52998224725");
        var bruno = pessoa("Bruno", "bruno@example.com", "11144477735");
        colaboradores.cadastrar(ana.getId(), "VEN-01", FuncaoColaborador.VENDEDOR, LocalDate.now());
        assertThrows(RecursoDuplicadoException.class,
                () -> colaboradores.cadastrar(bruno.getId(), " VEN-01 ", FuncaoColaborador.GERENTE, LocalDate.now()));
        var outro = colaboradores.cadastrar(bruno.getId(), "VEN-02", FuncaoColaborador.GERENTE, LocalDate.now());
        assertThrows(RecursoDuplicadoException.class,
                () -> colaboradores.alterar(outro.getId(), " VEN-01 ", FuncaoColaborador.ESTOQUISTA));
        assertEquals("VEN-02", outro.getMatricula());
    }

    @Test
    void deveRetornarNaoEncontradoParaPessoasEPapeisAusentes() {
        assertThrows(RecursoNaoEncontradoException.class, () -> pessoas.buscarPorId(Long.MAX_VALUE));
        assertThrows(RecursoNaoEncontradoException.class, () -> pessoas.alterar(Long.MAX_VALUE, "Ana", "a@b.com", "52998224725"));
        assertThrows(RecursoNaoEncontradoException.class, () -> pessoas.alterarStatus(Long.MAX_VALUE, Status.ATIVO));
        assertThrows(RecursoNaoEncontradoException.class, () -> clientes.buscarPorId(Long.MAX_VALUE));
        assertThrows(RecursoNaoEncontradoException.class, () -> clientes.alterar(Long.MAX_VALUE, null));
        assertThrows(RecursoNaoEncontradoException.class, () -> clientes.cadastrar(Long.MAX_VALUE, null));
        assertThrows(RecursoNaoEncontradoException.class, () -> colaboradores.buscarPorId(Long.MAX_VALUE));
        assertThrows(RecursoNaoEncontradoException.class, () -> colaboradores.alterar(Long.MAX_VALUE, "X", FuncaoColaborador.GERENTE));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> colaboradores.cadastrar(Long.MAX_VALUE, "X", FuncaoColaborador.GERENTE, LocalDate.now()));
    }

    @Test
    void devePesquisarPessoasEPapeisComFiltrosCombinados() {
        var ana = pessoa("Ana", "ana@example.com", "52998224725");
        var bruno = pessoa("Bruno", "bruno@example.com", "11144477735");
        clientes.cadastrar(ana.getId(), null);
        clientes.cadastrar(bruno.getId(), null);
        colaboradores.cadastrar(ana.getId(), "VEN-01", FuncaoColaborador.VENDEDOR, LocalDate.now());
        colaboradores.cadastrar(bruno.getId(), "EST-01", FuncaoColaborador.ESTOQUISTA, LocalDate.now());
        pessoas.alterarStatus(bruno.getId(), Status.INATIVO);
        var pagina = PageRequest.of(0, 1, Sort.by("id"));
        assertEquals(1, pessoas.pesquisar(" ANA ", ana.getCpf(), Status.ATIVO, pagina).getTotalElements());
        assertEquals(0, pessoas.pesquisar("Ana", null, Status.INATIVO, pagina).getTotalElements());
        assertEquals(1, clientes.pesquisar(" ANA ", ana.getCpf(), pagina).getTotalElements());
        assertEquals(1, colaboradores.pesquisar(" ANA ", " VEN-01 ", FuncaoColaborador.VENDEDOR, pagina).getTotalElements());
        var invalida = PageRequest.of(0, 1, Sort.by("pessoa.nome"));
        assertThrows(IllegalArgumentException.class, () -> pessoas.pesquisar(null, null, null, invalida));
        assertThrows(IllegalArgumentException.class, () -> clientes.pesquisar(null, null, invalida));
        assertThrows(IllegalArgumentException.class, () -> colaboradores.pesquisar(null, null, null, invalida));
    }
}
