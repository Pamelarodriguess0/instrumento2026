package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.curso.instrumento2026.domain.Status;
import java.time.LocalDate;

@Schema(description = "Identidade civil compartilhada pelos papéis comerciais")
public record PessoaResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Ana Souza") String nome,
        @Schema(example = "ana@example.com") String email,
        @Schema(description = "CPF com 11 dígitos e verificadores válidos, sem pontuação; não comprova titularidade", pattern = "^[0-9]{11}$", minLength = 11, maxLength = 11, example = "52998224725") String cpf,
        @Schema(example = "2026-10-28") LocalDate dataCadastro,
        @Schema(example = "ATIVO") Status status) {
}
