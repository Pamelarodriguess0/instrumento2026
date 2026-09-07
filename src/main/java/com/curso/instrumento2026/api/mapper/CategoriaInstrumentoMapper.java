package com.curso.instrumento2026.api.mapper;

import com.curso.instrumento2026.api.dto.CategoriaInstrumentoRequest;
import com.curso.instrumento2026.api.dto.CategoriaInstrumentoResponse;
import com.curso.instrumento2026.domain.CategoriaInstrumento;
import org.springframework.stereotype.Component;

@Component
public class CategoriaInstrumentoMapper {

    public CategoriaInstrumento toEntity(
            CategoriaInstrumentoRequest request) {

        return new CategoriaInstrumento(
                null,
                request.nome()
        );
    }

    public CategoriaInstrumentoResponse toResponse(
            CategoriaInstrumento categoria) {

        return new CategoriaInstrumentoResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getStatus()
        );
    }
}
