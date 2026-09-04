package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import org.apache.juli.logging.Log;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaInstrumentoRepository
     extends JpaRepository<CategoriaInstrumento, Long> {
    boolean existsByNomeIgnoreCase(String nome);

    Optional<CategoriaInstrumento> findByNomeIgnoreCase(String nome);
}
