
package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Dados necessários para registrar uma entrada ou saída de estoque")
public record MovimentacaoEstoqueRequest(

        @Schema(
                description = "Quantidade de instrumentos a movimentar. Deve ser maior que zero",
                example = "5.000",
                minimum = "0",
                exclusiveMinimum = true,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser maior que zero")
        BigDecimal quantidade

) {
}
