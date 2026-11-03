package com.curso.instrumento2026.api.dto;

import com.curso.instrumento2026.domain.StatusContaAcesso;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record StatusContaAcessoRequest(
        @NotNull(message = "Status é obrigatório") @Schema(example = "BLOQUEADA") StatusContaAcesso status) {
}
