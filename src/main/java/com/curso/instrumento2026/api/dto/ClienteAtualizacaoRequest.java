package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

@Schema(description = "Altera somente o telefone; não substitui a pessoa")
public record ClienteAtualizacaoRequest(
        @Size(max = 30) @Schema(example = "(11) 99999-0000") String telefone) {
}
