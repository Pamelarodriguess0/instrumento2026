package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.repository.ContaAcessoRepository;
import com.curso.instrumento2026.repository.PessoaRepository;
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

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContaAcessoPersistenciaTest {
    // Valor fictício: validação criptográfica pertence ao serviço no Checkpoint 3B.
    private static final String HASH_SINTETICO = "hash-sintetico-sem-credencial-real";
    @Autowired ContaAcessoRepository contas;
    @Autowired PessoaRepository pessoas;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    private Pessoa pessoa;
    private Pessoa outraPessoa;

    @BeforeEach
    void preparar() {
        pessoa = pessoas.save(new Pessoa("Ana acesso", "ana.acesso@example.com", "52998224725", LocalDate.now()));
        outraPessoa = pessoas.save(new Pessoa("Bruno acesso", "bruno.acesso@example.com", "11144477735", LocalDate.now()));
    }

    private ContaAcesso cadastrar() {
        return contas.saveAndFlush(new ContaAcesso(pessoa, "  ANA.Acesso  ", HASH_SINTETICO));
    }

    @Test
    void deveMapearPessoaLazyEPerfisEagerComValoresString() {
        em.persist(new Cliente(pessoa, null));
        em.persist(new Colaborador(pessoa, "ACESSO-01", FuncaoColaborador.ADMINISTRATIVO, LocalDate.now()));
        var conta = cadastrar();
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        em.flush();
        Long id = conta.getId();
        Long pessoaId = pessoa.getId();
        em.clear();
        ContaAcesso salva = em.find(ContaAcesso.class, id);
        var util = em.getEntityManagerFactory().getPersistenceUnitUtil();
        assertFalse(util.isLoaded(salva, "pessoa"));
        assertTrue(util.isLoaded(salva, "perfis"));
        assertEquals(pessoaId, salva.getPessoa().getId());
        assertEquals(Set.of(PerfilAcesso.OPERADOR, PerfilAcesso.ADMINISTRADOR), salva.getPerfis());
        assertEquals("ana.acesso", salva.getLogin());
        assertEquals(HASH_SINTETICO, salva.getSenhaHash());
        assertEquals("ATIVA", jdbc.queryForObject("SELECT status FROM conta_acesso WHERE id=?", String.class, id));
        assertEquals(Set.of("OPERADOR", "ADMINISTRADOR"), Set.copyOf(jdbc.queryForList(
                "SELECT perfil FROM conta_acesso_perfil WHERE conta_acesso_id=?", String.class, id)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana.acesso", "ANA.ACESSO", "  AnA.AcEsSo  "})
    void deveConsultarLoginIgnorandoCaixaEEspacosECarregarGrafo(String login) {
        Long id = cadastrar().getId();
        Long pessoaId = pessoa.getId();
        em.clear();
        assertTrue(contas.existsByLoginIgnoreCase(login));
        assertTrue(contas.existsByPessoaId(pessoaId));
        ContaAcesso encontrada = contas.findByLoginIgnoreCase(login).orElseThrow();
        assertTrue(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(encontrada, "pessoa"));
        em.clear();
        assertEquals(id, encontrada.getId());
        assertEquals("Ana acesso", encontrada.getPessoa().getNome());
        assertEquals(Set.of(PerfilAcesso.OPERADOR), encontrada.getPerfis());
        assertTrue(encontrada.podeAutenticar());
    }

    @Test
    void consultaDeveSeguirIndiceNormalizadoMesmoComLoginExternoComEspacos() {
        Long id = cadastrar().getId();
        jdbc.update("UPDATE conta_acesso SET login='  ANA.ACESSO  ' WHERE id=?", id);
        em.clear();
        assertTrue(contas.existsByLoginIgnoreCase(" ana.acesso "));
        assertEquals(id, contas.findByLoginIgnoreCase("ana.acesso").orElseThrow().getId());
    }

    @Test
    void deveRetornarAusenciaSemEncontrarOutraConta() {
        cadastrar();
        assertFalse(contas.existsByLoginIgnoreCase("inexistente"));
        assertFalse(contas.existsByPessoaId(outraPessoa.getId()));
        assertTrue(contas.findByLoginIgnoreCase("inexistente").isEmpty());
    }

    @Test
    void devePersistirBloqueioHashEPerfisSubstituidos() {
        var conta = cadastrar();
        conta.bloquear();
        conta.alterarSenha("outro-hash-sintetico");
        conta.substituirPerfis(Set.of(PerfilAcesso.ADMINISTRADOR));
        em.flush();
        em.clear();
        ContaAcesso salva = contas.findByLoginIgnoreCase("ana.acesso").orElseThrow();
        assertEquals(StatusContaAcesso.BLOQUEADA, salva.getStatus());
        assertEquals("outro-hash-sintetico", salva.getSenhaHash());
        assertEquals(Set.of(PerfilAcesso.ADMINISTRADOR), salva.getPerfis());
        assertFalse(salva.podeAutenticar());
    }

    @Test
    void deveImpedirSegundaContaParaMesmaPessoaNoBanco() {
        cadastrar();
        assertThrows(DataIntegrityViolationException.class,
                () -> contas.saveAndFlush(new ContaAcesso(pessoa, "outro.login", HASH_SINTETICO)));
    }

    @Test
    void deveImpedirLoginDuplicadoPeloIndiceNormalizadoDoBanco() {
        cadastrar();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO conta_acesso (pessoa_id,login,senha_hash,status) VALUES (?,?,?,'ATIVA')",
                outraPessoa.getId(), "  ANA.ACESSO  ", HASH_SINTETICO));
    }

    @ParameterizedTest
    @ValueSource(strings = {"status", "perfil", "perfil_duplicado", "fk_pessoa", "fk_conta", "hash_nulo"})
    void deveAplicarConstraintsDaMigracao006(String caso) {
        Long id = cadastrar().getId();
        assertThrows(DataIntegrityViolationException.class, () -> {
            switch (caso) {
                case "status" -> jdbc.update("UPDATE conta_acesso SET status='INVALIDO' WHERE id=?", id);
                case "perfil" -> jdbc.update("INSERT INTO conta_acesso_perfil VALUES (?,'INVALIDO')", id);
                case "perfil_duplicado" -> jdbc.update("INSERT INTO conta_acesso_perfil VALUES (?,'OPERADOR')", id);
                case "fk_pessoa" -> jdbc.update("UPDATE conta_acesso SET pessoa_id=? WHERE id=?", Long.MAX_VALUE, id);
                case "fk_conta" -> jdbc.update("INSERT INTO conta_acesso_perfil VALUES (?,'OPERADOR')", Long.MAX_VALUE);
                case "hash_nulo" -> jdbc.update("UPDATE conta_acesso SET senha_hash=NULL WHERE id=?", id);
                default -> throw new AssertionError(caso);
            }
        });
    }

    @Test
    void deveImpedirExclusaoDaPessoaReferenciada() {
        cadastrar();
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("DELETE FROM pessoa WHERE id=?", pessoa.getId()));
    }

    @Test
    void deveExcluirPerfisComContaSemExcluirPessoaOuPapeis() {
        em.persist(new Cliente(pessoa, null));
        em.persist(new Colaborador(pessoa, "ACESSO-DELETE", FuncaoColaborador.VENDEDOR, LocalDate.now()));
        Long id = cadastrar().getId();
        Long pessoaId = pessoa.getId();
        jdbc.update("DELETE FROM conta_acesso WHERE id=?", id);
        em.clear();
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM conta_acesso_perfil WHERE conta_acesso_id=?", Integer.class, id));
        assertTrue(pessoas.existsById(pessoaId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM cliente WHERE pessoa_id=?", Integer.class, pessoaId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM colaborador WHERE pessoa_id=?", Integer.class, pessoaId));
    }
}
