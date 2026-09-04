package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.domain.Instrumento;

import com.curso.instrumento2026.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstrumentoRepository
            extends JpaRepository<Instrumento,Long> {

    Optional<Instrumento> findByCodigoInstrumento(String codigoInstrumento);
    boolean existsByCodigoInstrumento(String codigoInstrumento);
    List<Instrumento> findByCategoriaId(Long categoriaId);
    List<Instrumento> findByStatus(Status status);
}
