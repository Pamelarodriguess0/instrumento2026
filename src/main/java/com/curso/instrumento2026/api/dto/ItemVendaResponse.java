package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Item com preço histórico capturado na inclusão e subtotal calculado")
public record ItemVendaResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long instrumentoId,
        @Schema(example = "Violino Eagle") String instrumentoNome,
        @Schema(example = "2") BigDecimal quantidade,
        @Schema(example = "1500.00") BigDecimal valorUnitario,
        @Schema(example = "3000.00") BigDecimal subtotal) {
}
