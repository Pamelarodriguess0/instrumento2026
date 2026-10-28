package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "Inclusão de instrumento em venda aberta, em unidades inteiras")
public record ItemVendaRequest(
        @NotNull @Positive @Schema(example = "1") Long instrumentoId,
        @NotNull @DecimalMin("0.001") @Digits(integer = 15, fraction = 3) @Schema(description = "Quantidade inteira positiva de instrumentos; aceita 2 ou 2.000, rejeita frações", type = "integer", format = "int32", minimum = "1", maximum = "2147483647", multipleOf = 1, example = "2") BigDecimal quantidade) {
}
