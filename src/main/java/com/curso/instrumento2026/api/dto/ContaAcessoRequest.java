package com.curso.instrumento2026.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.Locale;

public record ContaAcessoRequest(
        @NotNull(message = "Pessoa é obrigatória") @Schema(example = "1") Long pessoaId,
        @NotBlank(message = "Login é obrigatório")
        @Size(min = 3, max = 80, message = "Login deve possuir entre 3 e 80 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "Login aceita somente letras, números, ponto, hífen e sublinhado")
        @Schema(example = "operador.loja") String login,
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 12, max = 72, message = "Senha deve possuir entre 12 e 72 caracteres")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password",
                description = "Mínimo de 12 caracteres e máximo efetivo de 72 bytes UTF-8") String senha) {
    public ContaAcessoRequest {
        login = login == null ? null : login.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "ContaAcessoRequest[pessoaId=" + pessoaId + ", login=" + login + ", senha=[PROTEGIDA]]";
    }
}
