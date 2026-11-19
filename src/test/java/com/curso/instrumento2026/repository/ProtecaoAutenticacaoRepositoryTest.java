package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.support.ProtecaoAutenticacaoPersistenciaSupport;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ProtecaoAutenticacaoRepositoryTest extends ProtecaoAutenticacaoPersistenciaSupport {
    @Autowired ProtecaoAutenticacaoRepository repository;

    @Test
    void upsertDeveCriarUmaLinhaENaoDuplicarNemReiniciarEstadoExistente() {
        String chave = novaChave();
        assertEquals(1, repository.inserirSeAusente(chave));
        var protecao = repository.buscarParaAtualizacao(chave).orElseThrow();
        assertEquals(0, protecao.getFalhasConsecutivas());
        assertNull(protecao.getJanelaIniciadaEm());
        assertNull(protecao.getBloqueadaAte());
        for (int i = 0; i < 5; i++) protecao.registrarFalha(INICIO, 5, PRAZO, PRAZO);
        em.flush();
        assertEquals(0, repository.inserirSeAusente(chave));
        em.clear();
        var salva = repository.buscarParaAtualizacao(chave).orElseThrow();
        assertEquals(5, salva.getFalhasConsecutivas());
        assertEquals(INICIO, salva.getJanelaIniciadaEm());
        assertEquals(INICIO.plus(PRAZO), salva.getBloqueadaAte());
        assertEquals(1, jdbc.queryForObject("select count(*) from protecao_autenticacao where chave_login=?",
                Integer.class, chave));
    }

    @Test
    void buscaDeveUsarChaveExataELockPessimista() {
        String chave = novaChave();
        repository.inserirSeAusente(chave);
        var protecao = repository.buscarParaAtualizacao(chave).orElseThrow();
        assertEquals(LockModeType.PESSIMISTIC_WRITE, em.getLockMode(protecao));
        assertEquals(chave, protecao.getChaveLogin());
        assertTrue(repository.buscarParaAtualizacao(" " + chave).isEmpty());
        assertTrue(repository.buscarParaAtualizacao(novaChave()).isEmpty());
    }
}
