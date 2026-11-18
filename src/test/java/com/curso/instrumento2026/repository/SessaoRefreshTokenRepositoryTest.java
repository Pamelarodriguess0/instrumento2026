package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessaoRefreshTokenRepositoryTest {
    private static final Instant CRIACAO = Instant.parse("2026-11-17T12:00:00Z");
    private static final Instant REVOGACAO = CRIACAO.plusSeconds(60);
    @Autowired EntityManager em;
    @Autowired SessaoRefreshTokenRepository repository;

    private ContaAcesso conta(int numero) {
        var pessoa = new Pessoa("Pessoa repository " + numero, "repo.sessao." + numero + "@example.com",
                numero == 1 ? "52998224725" : "11144477735", LocalDate.of(2026, 11, 17));
        em.persist(pessoa);
        var conta = new ContaAcesso(pessoa, "repo.sessao." + numero, "hash-sintetico-sem-credencial-real");
        em.persist(conta);
        return conta;
    }

    private SessaoRefreshToken sessao(ContaAcesso conta, int familia, int numeroHash) {
        var sessao = new SessaoRefreshToken(conta, new UUID(0, familia).toString(),
                String.format("%064x", numeroHash), CRIACAO, CRIACAO, CRIACAO.plusSeconds(28800));
        em.persist(sessao);
        return sessao;
    }

    @Test
    void deveBuscarHashExatoComLockECarregarContaPessoaEPerfis() {
        var conta = conta(1);
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        var sessao = sessao(conta, 1, 1);
        String hash = sessao.getTokenHash();
        Long id = sessao.getId();
        em.flush();
        em.clear();

        var encontrada = repository.buscarPorHashParaAtualizacao(hash).orElseThrow();

        assertEquals(id, encontrada.getId());
        assertEquals(LockModeType.PESSIMISTIC_WRITE, em.getLockMode(encontrada));
        var util = em.getEntityManagerFactory().getPersistenceUnitUtil();
        assertTrue(util.isLoaded(encontrada, "conta"));
        assertTrue(util.isLoaded(encontrada.getConta(), "pessoa"));
        assertTrue(util.isLoaded(encontrada.getConta(), "perfis"));
        assertTrue(encontrada.getConta().possuiPerfil(PerfilAcesso.ADMINISTRADOR));
        assertTrue(repository.buscarPorHashParaAtualizacao(" " + hash).isEmpty());
        assertTrue(repository.buscarPorHashParaAtualizacao("f".repeat(64)).isEmpty());
    }

    @Test
    void deveEncontrarGeracaoUtilizadaERevogadaParaFuturaDeteccaoDeReplay() {
        var sessao = sessao(conta(1), 1, 1);
        sessao.marcarUtilizada(CRIACAO.plusSeconds(1));
        sessao.revogar(REVOGACAO, MotivoRevogacaoRefreshToken.LOGOUT);
        String hash = sessao.getTokenHash();
        em.flush();
        em.clear();

        var encontrada = repository.buscarPorHashParaAtualizacao(hash).orElseThrow();

        assertTrue(encontrada.foiUtilizada());
        assertTrue(encontrada.foiRevogada());
        assertEquals(MotivoRevogacaoRefreshToken.LOGOUT, encontrada.getMotivoRevogacao());
    }

    @Test
    void deveRevogarSomenteFamiliaSolicitadaComFlushELimpezaDoContexto() {
        var conta = conta(1);
        var primeira = sessao(conta, 1, 1);
        var segunda = sessao(conta, 1, 2);
        var outraFamilia = sessao(conta, 2, 3);
        primeira.marcarUtilizada(CRIACAO.plusSeconds(1));
        Long primeiraId = primeira.getId();
        Long segundaId = segunda.getId();
        Long outraId = outraFamilia.getId();

        int alteradas = repository.revogarFamilia(primeira.getFamiliaId(), REVOGACAO,
                MotivoRevogacaoRefreshToken.REUTILIZACAO);

        assertEquals(2, alteradas);
        assertFalse(em.contains(primeira));
        assertEquals(CRIACAO.plusSeconds(1), em.find(SessaoRefreshToken.class, primeiraId).getUtilizadaEm());
        assertEquals(REVOGACAO, em.find(SessaoRefreshToken.class, primeiraId).getRevogadaEm());
        assertEquals(MotivoRevogacaoRefreshToken.REUTILIZACAO,
                em.find(SessaoRefreshToken.class, segundaId).getMotivoRevogacao());
        assertFalse(em.find(SessaoRefreshToken.class, outraId).foiRevogada());
        assertEquals(0, repository.revogarFamilia(primeira.getFamiliaId(), REVOGACAO.plusSeconds(1),
                MotivoRevogacaoRefreshToken.LOGOUT));
        assertEquals(REVOGACAO, em.find(SessaoRefreshToken.class, primeiraId).getRevogadaEm());
        assertEquals(MotivoRevogacaoRefreshToken.REUTILIZACAO,
                em.find(SessaoRefreshToken.class, primeiraId).getMotivoRevogacao());
    }

    @Test
    void deveRevogarFamiliasDaContaSemAfetarOutraContaOuPrimeiraRevogacao() {
        var conta = conta(1);
        Long contaId = conta.getId();
        var primeira = sessao(conta, 1, 1);
        var segunda = sessao(conta, 2, 2);
        var jaRevogada = sessao(conta, 3, 3);
        jaRevogada.revogar(CRIACAO.plusSeconds(1), MotivoRevogacaoRefreshToken.LOGOUT);
        var externa = sessao(conta(2), 4, 4);
        Long primeiraId = primeira.getId();
        Long segundaId = segunda.getId();
        Long revogadaId = jaRevogada.getId();
        Long externaId = externa.getId();

        assertEquals(2, repository.revogarTodasDaConta(contaId, REVOGACAO,
                MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL));

        assertEquals(MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL,
                em.find(SessaoRefreshToken.class, primeiraId).getMotivoRevogacao());
        assertEquals(REVOGACAO, em.find(SessaoRefreshToken.class, segundaId).getRevogadaEm());
        assertEquals(MotivoRevogacaoRefreshToken.LOGOUT,
                em.find(SessaoRefreshToken.class, revogadaId).getMotivoRevogacao());
        assertEquals(CRIACAO.plusSeconds(1), em.find(SessaoRefreshToken.class, revogadaId).getRevogadaEm());
        assertFalse(em.find(SessaoRefreshToken.class, externaId).foiRevogada());
        assertEquals(0, repository.revogarTodasDaConta(contaId, REVOGACAO.plusSeconds(1),
                MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL));
        assertEquals(0, repository.revogarTodasDaConta(Long.MAX_VALUE, REVOGACAO,
                MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL));
    }
}
