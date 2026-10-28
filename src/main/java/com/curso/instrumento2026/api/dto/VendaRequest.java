package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

@Schema(description = "Abre venda para cliente ativo e vendedor ou gerente ativo")
public record VendaRequest(
        @NotNull @Positive @Schema(example = "1") Long clienteId,
        @NotNull @Positive @Schema(example = "2") Long vendedorId) {
}
