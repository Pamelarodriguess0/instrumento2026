package com.curso.instrumento2026.api.dto;
import com.curso.instrumento2026.domain.Status;

public record CategoriaInstrumentoResponse(
        Long id,
        String nome,
        Status status

) {
}
