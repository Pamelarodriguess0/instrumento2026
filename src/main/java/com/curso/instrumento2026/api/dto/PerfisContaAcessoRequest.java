package com.curso.instrumento2026.api.dto;

import com.curso.instrumento2026.domain.PerfilAcesso;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record PerfisContaAcessoRequest(
        @NotEmpty(message = "Informe ao menos um perfil")
        @Schema(description = "Conjunto não vazio de perfis permitidos", example = "[\"OPERADOR\",\"ADMINISTRADOR\"]")
        Set<@NotNull(message = "Perfil é obrigatório") PerfilAcesso> perfis) {
}
