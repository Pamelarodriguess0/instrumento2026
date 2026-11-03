package com.curso.instrumento2026.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaContaRequest(
        @NotBlank(message = "Senha atual é obrigatória")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password") String senhaAtual,
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 12, max = 72, message = "Nova senha deve possuir entre 12 e 72 caracteres")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password",
                description = "Mínimo de 12 caracteres e máximo efetivo de 72 bytes UTF-8") String novaSenha) {
    @Override
    public String toString() {
        return "AlterarSenhaContaRequest[senhaAtual=[PROTEGIDA], novaSenha=[PROTEGIDA]]";
    }
}
