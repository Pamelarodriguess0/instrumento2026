
package com.curso.instrumento2026.api.dto;

import com.curso.instrumento2026.domain.Status;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de uma categoria de instrumentos musicais retornados pela API")
public record CategoriaInstrumentoResponse(

        @Schema(
                description = "Identificador da categoria",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Nome da categoria",
                example = "Instrumentos de Cordas"
        )
        String nome,

        @Schema(
                description = "Status atual da categoria",
                example = "ATIVO"
        )
        Status status

) {
}
