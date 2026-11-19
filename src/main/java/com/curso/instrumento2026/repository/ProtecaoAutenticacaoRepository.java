package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.domain.ProtecaoAutenticacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProtecaoAutenticacaoRepository extends JpaRepository<ProtecaoAutenticacao, Long> {
    @Modifying
    @Query(value = """
            INSERT INTO protecao_autenticacao (chave_login, falhas_consecutivas)
            VALUES (:chaveLogin, 0)
            ON CONFLICT (chave_login) DO NOTHING
            """, nativeQuery = true)
    int inserirSeAusente(@Param("chaveLogin") String chaveLogin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select protecao from ProtecaoAutenticacao protecao
            where protecao.chaveLogin = :chaveLogin
            """)
    Optional<ProtecaoAutenticacao> buscarParaAtualizacao(@Param("chaveLogin") String chaveLogin);
}
