
package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para atualizar uma categoria de instrumentos musicais")
public record CategoriaInstrumentoAtualizacaoRequest(

        @Schema(
                description = "Novo nome da categoria",
                example = "Instrumentos de Cordas",
                maxLength = 120,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome

) {
}
