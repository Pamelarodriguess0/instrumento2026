package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.ProtecaoAutenticacao;
import com.curso.instrumento2026.support.ProtecaoAutenticacaoPersistenciaSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ProtecaoAutenticacaoPersistenciaTest extends ProtecaoAutenticacaoPersistenciaSupport {
    @Test
    void devePersistirERelerProtecaoSemAlterarRegrasTemporais() {
        var protecao = new ProtecaoAutenticacao(novaChave());
        em.persist(protecao);
        for (int i = 0; i < 5; i++) protecao.registrarFalha(INICIO, 5, PRAZO, PRAZO);
        em.flush();
        em.clear();
        var salva = em.find(ProtecaoAutenticacao.class, protecao.getId());
        assertEquals(protecao.getChaveLogin(), salva.getChaveLogin());
        assertEquals(5, salva.getFalhasConsecutivas());
        assertEquals(INICIO, salva.getJanelaIniciadaEm());
        assertEquals(INICIO.plus(PRAZO), salva.getBloqueadaAte());
        assertTrue(salva.estaBloqueada(INICIO.plus(PRAZO).minusNanos(1)));
        assertFalse(salva.estaBloqueada(INICIO.plus(PRAZO)));
        salva.registrarSucesso();
        em.flush();
        em.clear();
        var limpa = em.find(ProtecaoAutenticacao.class, protecao.getId());
        assertEquals(0, limpa.getFalhasConsecutivas());
        assertNull(limpa.getJanelaIniciadaEm());
        assertNull(limpa.getBloqueadaAte());
    }

    @Test
    void deveRegistrarTresNovosChangeSetsEPreservarOsQuarentaEQuatroAnteriores() {
        assertEquals(44, jdbc.queryForObject("select count(*) from databasechangelog where filename <> "
                + "'db/changelog/changes/008-auditoria-protecao-autenticacao.yaml'", Integer.class));
        assertEquals(3, jdbc.queryForObject("select count(*) from databasechangelog where filename = "
                + "'db/changelog/changes/008-auditoria-protecao-autenticacao.yaml'", Integer.class));
        assertEquals(47, jdbc.queryForObject("select count(*) from databasechangelog", Integer.class));
        assertFalse(jdbc.queryForObject("select locked from databasechangeloglock", Boolean.class));
    }

    @Test
    void deveCriarColunasComTiposTamanhosENulabilidadePrevistos() {
        conferirColunas("protecao_autenticacao", Map.of(
                "id", "bigint:NO:", "chave_login", "character varying:NO:64",
                "falhas_consecutivas", "integer:NO:",
                "janela_iniciada_em", "timestamp with time zone:YES:",
                "bloqueada_ate", "timestamp with time zone:YES:"));
        conferirColunas("evento_seguranca", Map.of(
                "id", "bigint:NO:", "tipo", "character varying:NO:40",
                "ocorrido_em", "timestamp with time zone:NO:", "ator_conta_id", "bigint:YES:",
                "alvo_conta_id", "bigint:YES:", "identificador_hash", "character varying:YES:64",
                "endereco_origem", "character varying:YES:45"));
    }

    private void conferirColunas(String tabela, Map<String, String> esperadas) {
        var encontradas = jdbc.query("""
                select column_name, data_type || ':' || is_nullable || ':' ||
                    coalesce(character_maximum_length::text, '') as assinatura
                from information_schema.columns where table_schema='public' and table_name=?
                """, rs -> {
            var mapa = new java.util.HashMap<String, String>();
            while (rs.next()) mapa.put(rs.getString(1), rs.getString(2));
            return mapa;
        }, tabela);
        assertEquals(esperadas, encontradas);
    }

    @Test
    void deveCriarChavesChecksEIndicesSemForeignKeysNaAuditoria() {
        var constraints = jdbc.queryForList("""
                select conname from pg_constraint where conrelid in
                ('public.protecao_autenticacao'::regclass, 'public.evento_seguranca'::regclass)
                """, String.class);
        assertTrue(constraints.containsAll(java.util.List.of("pk_protecao_autenticacao",
                "uk_protecao_autenticacao_chave", "ck_protecao_autenticacao_falhas",
                "pk_evento_seguranca", "ck_evento_seguranca_tipo")));
        assertEquals(0, jdbc.queryForObject("select count(*) from pg_constraint "
                + "where conrelid='public.evento_seguranca'::regclass and contype='f'", Integer.class));
        for (var indice : Map.of("tipo", "tipo", "ator", "ator_conta_id", "alvo", "alvo_conta_id").entrySet()) {
            String definicao = jdbc.queryForObject("select indexdef from pg_indexes where schemaname='public' "
                    + "and indexname=?", String.class, "idx_evento_seguranca_" + indice.getKey() + "_data");
            assertNotNull(definicao);
            assertTrue(definicao.contains("(" + indice.getValue() + ", ocorrido_em DESC)"));
        }
    }

    @Test
    void deveAplicarDefaultZeroEAceitarDatasNulas() {
        String chave = novaChave();
        jdbc.update("insert into protecao_autenticacao(chave_login) values (?)", chave);
        var protecao = jdbc.queryForMap("select * from protecao_autenticacao where chave_login=?", chave);
        assertNotNull(protecao.get("id"));
        assertEquals(0, protecao.get("falhas_consecutivas"));
        assertNull(protecao.get("janela_iniciada_em"));
        assertNull(protecao.get("bloqueada_ate"));
    }

    @Test
    void deveRejeitarChaveDuplicada() {
        String chave = novaChave();
        jdbc.update("insert into protecao_autenticacao(chave_login) values (?)", chave);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into protecao_autenticacao(chave_login) values (?)", chave));
    }

    @Test
    void deveRejeitarContadorNegativo() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into protecao_autenticacao(chave_login,falhas_consecutivas) values (?,-1)", novaChave()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOGIN_SUCESSO", "LOGIN_FALHA", "LOGIN_RECUSADO_BLOQUEIO",
            "BLOQUEIO_TEMPORARIO_ATIVADO", "CONTA_CRIADA", "SENHA_ALTERADA", "PERFIS_ALTERADOS",
            "STATUS_ALTERADO", "REFRESH_SUCESSO", "REFRESH_FALHA", "LOGOUT", "LOGOUT_GLOBAL"})
    void deveAceitarTiposPrevistosEIdsHistoricosSemConta(String tipo) {
        Long id = jdbc.queryForObject("""
                insert into evento_seguranca(tipo,ocorrido_em,ator_conta_id,alvo_conta_id)
                values (?,TIMESTAMPTZ '2026-11-18 12:00:00+00',-17001,-17002) returning id
                """, Long.class, tipo);
        var evento = jdbc.queryForMap("select * from evento_seguranca where id=?", id);
        assertEquals(tipo, evento.get("tipo"));
        assertEquals(-17001L, evento.get("ator_conta_id"));
        assertEquals(-17002L, evento.get("alvo_conta_id"));
        assertNull(evento.get("identificador_hash"));
        assertNull(evento.get("endereco_origem"));
    }

    @Test
    void deveRejeitarTipoDeEventoDesconhecido() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                insert into evento_seguranca(tipo,ocorrido_em) values ('TIPO_INVALIDO',now())
                """));
    }

    @ParameterizedTest
    @ValueSource(strings = {"insert into protecao_autenticacao(chave_login) values (null)",
            "insert into protecao_autenticacao(chave_login,falhas_consecutivas) values ('chave-sintetica',null)",
            "insert into evento_seguranca(tipo,ocorrido_em) values (null,now())",
            "insert into evento_seguranca(tipo,ocorrido_em) values ('LOGIN_FALHA',null)"})
    void deveRejeitarNulosEmColunasObrigatorias(String sql) {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql));
    }
}
