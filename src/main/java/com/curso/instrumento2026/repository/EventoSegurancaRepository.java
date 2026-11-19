package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.domain.EventoSeguranca;
import com.curso.instrumento2026.domain.TipoEventoSeguranca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoSegurancaRepository extends JpaRepository<EventoSeguranca, Long> {
    Page<EventoSeguranca> findByTipo(TipoEventoSeguranca tipo, Pageable pageable);
}
