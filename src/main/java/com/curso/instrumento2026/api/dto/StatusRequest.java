
package com.curso.instrumento2026.api.dto;

import com.curso.instrumento2026.domain.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados necessários para alterar o status de um instrumento ou categoria")
public record StatusRequest(

        @Schema(
                description = "Novo status do registro",
                example = "ATIVO",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Status é obrigatório")
        Status status

) {
}
