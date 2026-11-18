package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

// Preparado no ponto A. Executar somente após autorização e conferência do banco test.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessaoRefreshTokenPersistenciaTest {
    private static final Instant CRIACAO = Instant.parse("2026-11-16T12:00:00Z");
    private static final String FAMILIA = "00000000-0000-0000-0000-000000000016";
    private static final String HASH_SINTETICO = "a".repeat(64);
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    private ContaAcesso conta() {
        var pessoa = new Pessoa("Pessoa sessão", "sessao.persistencia@example.com", "52998224725",
                LocalDate.of(2026, 11, 16));
        em.persist(pessoa);
        var conta = new ContaAcesso(pessoa, "sessao.persistencia", "hash-sintetico-sem-credencial-real");
        em.persist(conta);
        em.flush();
        return conta;
    }

    private SessaoRefreshToken sessao(ContaAcesso conta) {
        var sessao = new SessaoRefreshToken(conta, FAMILIA, HASH_SINTETICO, CRIACAO, CRIACAO,
                CRIACAO.plusSeconds(28800));
        em.persist(sessao);
        em.flush();
        return sessao;
    }

    private void inserirDiretamente(Long contaId, String hash, String expiracao, String motivo) {
        jdbc.update("""
                INSERT INTO sessao_refresh_token
                    (conta_acesso_id, familia_id, token_hash, criada_em, ultima_atividade_em,
                     expira_em, motivo_revogacao)
                VALUES (?, ?, ?, TIMESTAMPTZ '2026-11-16 12:00:00+00',
                        TIMESTAMPTZ '2026-11-16 12:00:00+00', CAST(? AS TIMESTAMP WITH TIME ZONE), ?)
                """, contaId, FAMILIA, hash, expiracao, motivo);
    }

    @Test
    void devePersistirERelerGeracaoComContaLazyEEstadoDeUsoERevogacao() {
        var conta = conta();
        Long pessoaId = conta.getPessoa().getId();
        var sessao = sessao(conta);
        sessao.marcarUtilizada(CRIACAO.plusSeconds(1));
        sessao.revogar(CRIACAO.plusSeconds(2), MotivoRevogacaoRefreshToken.REUTILIZACAO);
        em.flush();
        Long id = sessao.getId();
        em.clear();
        var salva = em.find(SessaoRefreshToken.class, id);
        assertFalse(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(salva, "conta"));
        assertEquals(conta.getId(), salva.getConta().getId());
        assertEquals(pessoaId, salva.getConta().getPessoa().getId());
        assertEquals(FAMILIA, salva.getFamiliaId());
        assertEquals(HASH_SINTETICO, salva.getTokenHash());
        assertEquals(CRIACAO, salva.getCriadaEm());
        assertEquals(CRIACAO, salva.getUltimaAtividadeEm());
        assertEquals(CRIACAO.plusSeconds(28800), salva.getExpiraEm());
        assertEquals(CRIACAO.plusSeconds(1), salva.getUtilizadaEm());
        assertEquals(CRIACAO.plusSeconds(2), salva.getRevogadaEm());
        assertEquals(MotivoRevogacaoRefreshToken.REUTILIZACAO, salva.getMotivoRevogacao());
        assertEquals(HASH_SINTETICO, jdbc.queryForObject(
                "SELECT token_hash FROM sessao_refresh_token WHERE id=?", String.class, id));
    }

    @Test
    void deveRegistrarTresChangeSetsSemReaplicarOsQuarentaEUmAnteriores() {
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM databasechangelog "
                + "WHERE filename='db/changelog/changes/007-sessoes-refresh-token.yaml'", Integer.class));
        assertEquals(41, jdbc.queryForObject("SELECT count(*) FROM databasechangelog "
                + "WHERE filename <> 'db/changelog/changes/007-sessoes-refresh-token.yaml'", Integer.class));
        assertEquals(44, jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class));
    }

    @Test
    void deveImpedirHashDuplicadoNoBanco() {
        var conta = conta();
        sessao(conta);
        assertThrows(DataIntegrityViolationException.class, () -> inserirDiretamente(
                conta.getId(), HASH_SINTETICO, "2026-11-16T20:00:00Z", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-16T11:59:59Z", "2026-11-16T12:00:00Z"})
    void deveImpedirExpiracaoNaoPosteriorACriacaoNoBanco(String expiracao) {
        var conta = conta();
        assertThrows(DataIntegrityViolationException.class,
                () -> inserirDiretamente(conta.getId(), HASH_SINTETICO, expiracao, null));
    }

    @Test
    void deveImpedirMotivoDesconhecidoNoBanco() {
        var conta = conta();
        assertThrows(DataIntegrityViolationException.class, () -> inserirDiretamente(
                conta.getId(), HASH_SINTETICO, "2026-11-16T20:00:00Z", "MOTIVO_INVALIDO"));
    }

    @Test
    void deveExigirContaExistenteNoBanco() {
        assertThrows(DataIntegrityViolationException.class, () -> inserirDiretamente(
                Long.MAX_VALUE, HASH_SINTETICO, "2026-11-16T20:00:00Z", null));
    }

    @Test
    void exclusaoDaContaDeveExcluirSessoesSemExcluirPessoa() {
        var conta = conta();
        Long pessoaId = conta.getPessoa().getId();
        var sessao = sessao(conta);
        jdbc.update("DELETE FROM conta_acesso WHERE id=?", conta.getId());
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM sessao_refresh_token WHERE id=?", Integer.class, sessao.getId()));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pessoa WHERE id=?", Integer.class, pessoaId));
    }

    @Test
    void deveCriarIndicesDeFamiliaEContaESomenteColunaDeHash() {
        var indices = jdbc.queryForList("SELECT indexname FROM pg_indexes "
                + "WHERE schemaname=current_schema() AND tablename='sessao_refresh_token'", String.class);
        assertTrue(indices.contains("idx_sessao_refresh_token_familia"));
        assertTrue(indices.contains("idx_sessao_refresh_token_conta"));
        assertTrue(indices.contains("uk_sessao_refresh_token_hash"));
        var colunas = jdbc.queryForList("SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema=current_schema() AND table_name='sessao_refresh_token'", String.class);
        assertEquals(10, colunas.size());
        assertTrue(colunas.contains("token_hash"));
        assertFalse(colunas.contains("refresh_token"));
    }
}
