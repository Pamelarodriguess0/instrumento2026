package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.curso.instrumento2026.domain.FuncaoColaborador;
import java.time.LocalDate;

@Schema(description = "Vínculo profissional; função não representa permissão de acesso")
public record ColaboradorResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long pessoaId,
        @Schema(example = "Ana Souza") String nome,
        @Schema(example = "ana@example.com") String email,
        @Schema(description = "CPF com 11 dígitos e verificadores válidos, sem pontuação; não comprova titularidade", pattern = "^[0-9]{11}$", minLength = 11, maxLength = 11, example = "52998224725") String cpf,
        @Schema(example = "VEN-001") String matricula,
        @Schema(example = "VENDEDOR") FuncaoColaborador funcao,
        @Schema(example = "2026-09-29") LocalDate dataAdmissao) {
}
