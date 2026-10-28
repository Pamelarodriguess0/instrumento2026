package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;



@Schema(description = "Cliente e dados de sua pessoa")
public record ClienteResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long pessoaId,
        @Schema(example = "Ana Souza") String nome,
        @Schema(example = "ana@example.com") String email,
        @Schema(description = "CPF com 11 dígitos e verificadores válidos, sem pontuação; não comprova titularidade", pattern = "^[0-9]{11}$", minLength = 11, maxLength = 11, example = "52998224725") String cpf,
        @Schema(example = "(11) 99999-0000") String telefone) {
}
