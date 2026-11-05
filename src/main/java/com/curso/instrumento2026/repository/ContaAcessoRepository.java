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

    @Query("select count(distinct conta) from ContaAcesso conta join conta.perfis perfil "
            + "where perfil = com.curso.instrumento2026.domain.PerfilAcesso.ADMINISTRADOR "
            + "and conta.status = com.curso.instrumento2026.domain.StatusContaAcesso.ATIVA "
            + "and conta.pessoa.status = com.curso.instrumento2026.domain.Status.ATIVO")
    long contarAdministradoresAtivos();

    @EntityGraph(attributePaths = {"pessoa", "perfis"})
    Optional<ContaAcesso> findOneById(Long id);

    @EntityGraph(attributePaths = {"pessoa", "perfis"})
    @Query("select conta from ContaAcesso conta where lower(trim(conta.login)) = lower(trim(:login))")
    Optional<ContaAcesso> findByLoginIgnoreCase(@Param("login") String login);
}
