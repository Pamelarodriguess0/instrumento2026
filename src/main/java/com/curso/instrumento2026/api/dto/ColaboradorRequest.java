package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import com.curso.instrumento2026.domain.FuncaoColaborador;
import java.time.LocalDate;

@Schema(description = "Cria vínculo profissional para uma pessoa ativa")
public record ColaboradorRequest(
        @NotNull @Positive @Schema(example = "1") Long pessoaId,
        @NotBlank @Size(max = 30) @Schema(example = "VEN-001") String matricula,
        @NotNull @Schema(example = "VENDEDOR") FuncaoColaborador funcao,
        @NotNull @Schema(example = "2026-09-29") LocalDate dataAdmissao) {
}
