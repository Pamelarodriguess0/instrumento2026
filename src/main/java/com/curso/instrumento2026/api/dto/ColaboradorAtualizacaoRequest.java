package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import com.curso.instrumento2026.domain.FuncaoColaborador;

@Schema(description = "Altera matrícula e função; preserva pessoa e data de admissão")
public record ColaboradorAtualizacaoRequest(
        @NotBlank @Size(max = 30) @Schema(example = "VEN-001") String matricula,
        @NotNull @Schema(example = "VENDEDOR") FuncaoColaborador funcao) {
}
