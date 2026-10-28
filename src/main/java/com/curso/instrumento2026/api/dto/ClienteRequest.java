package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

@Schema(description = "Associa uma pessoa ativa ao papel de cliente")
public record ClienteRequest(
        @NotNull @Positive @Schema(example = "1") Long pessoaId, @Size(max = 30) @Schema(example = "(11) 99999-0000") String telefone) {
}
