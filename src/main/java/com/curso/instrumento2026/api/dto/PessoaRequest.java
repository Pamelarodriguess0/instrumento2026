package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import com.curso.instrumento2026.api.validation.CpfValido;

@Schema(description = "Dados civis; CPF sem pontuação, com dígitos verificadores válidos")
public record PessoaRequest(
        @NotBlank @Size(max = 150) @Schema(example = "Ana Souza") String nome,
        @NotBlank @Email @Size(max = 180) @Schema(example = "ana@example.com") String email,
        @NotBlank @CpfValido @Schema(description = "CPF com 11 dígitos e verificadores válidos, sem pontuação; não comprova titularidade", pattern = "^[0-9]{11}$", minLength = 11, maxLength = 11, example = "52998224725") String cpf) {
}
