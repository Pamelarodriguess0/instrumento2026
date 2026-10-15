
package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoriaInstrumentoRepository
        extends JpaRepository<CategoriaInstrumento, Long>,
        JpaSpecificationExecutor<CategoriaInstrumento> {

    @Query(value = "SELECT EXISTS (SELECT 1 FROM categoria_instrumento "
            + "WHERE LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome)))", nativeQuery = true)
    boolean existsByNomeIgnoreCase(@Param("nome") String nome);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM categoria_instrumento "
            + "WHERE LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome)) AND id <> :id)", nativeQuery = true)
    boolean existsByNomeIgnoreCaseAndIdNot(@Param("nome") String nome, @Param("id") Long id);

    @Query(value = "SELECT * FROM categoria_instrumento "
            + "WHERE LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome))", nativeQuery = true)
    Optional<CategoriaInstrumento> findByNomeIgnoreCase(@Param("nome") String nome);
}
