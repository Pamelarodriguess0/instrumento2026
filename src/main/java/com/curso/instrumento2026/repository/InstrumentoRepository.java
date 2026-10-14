
package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.domain.Status;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface InstrumentoRepository
        extends JpaRepository<Instrumento, Long>,
        JpaSpecificationExecutor<Instrumento> {

    Optional<Instrumento> findByCodigoInstrumento(String codigo);
    boolean existsByCodigoInstrumento(String codigo);
    List<Instrumento> findByCategoriaId(Long categoriaId);
    List<Instrumento> findByStatus(Status status);

    @Override
    @EntityGraph(attributePaths = {"categoria", "fornecedor"})
    Optional<Instrumento> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"categoria", "fornecedor"})
    List<Instrumento> findAll();
}
