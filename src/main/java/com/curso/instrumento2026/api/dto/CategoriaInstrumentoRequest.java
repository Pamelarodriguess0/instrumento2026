
package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para cadastrar uma categoria de instrumentos musicais")
public record CategoriaInstrumentoRequest(

        @Schema(
                description = "Nome da categoria de instrumentos musicais",
                example = "Instrumentos de Sopro",
                maxLength = 120,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Nome da categoria é obrigatório")
        @Size(max = 120, message = "Nome da categoria deve possuir no máximo 120 caracteres")
        String nome

) {
}
