package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.domain.ContaAcesso;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContaAcessoRepository extends JpaRepository<ContaAcesso, Long> {
    @Query("select count(conta) > 0 from ContaAcesso conta "
            + "where lower(trim(conta.login)) = lower(trim(:login))")
    boolean existsByLoginIgnoreCase(@Param("login") String login);

    boolean existsByPessoaId(Long pessoaId);

    @EntityGraph(attributePaths = {"pessoa", "perfis"})
    Optional<ContaAcesso> findOneById(Long id);

    @EntityGraph(attributePaths = {"pessoa", "perfis"})
    @Query("select conta from ContaAcesso conta where lower(trim(conta.login)) = lower(trim(:login))")
    Optional<ContaAcesso> findByLoginIgnoreCase(@Param("login") String login);
}
