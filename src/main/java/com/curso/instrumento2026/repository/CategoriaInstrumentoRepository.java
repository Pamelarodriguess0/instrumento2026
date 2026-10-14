
package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CategoriaInstrumentoRepository
        extends JpaRepository<CategoriaInstrumento, Long>,
        JpaSpecificationExecutor<CategoriaInstrumento> {

    boolean existsByNomeIgnoreCase(String nome);
    Optional<CategoriaInstrumento> findByNomeIgnoreCase(String nome);
}
