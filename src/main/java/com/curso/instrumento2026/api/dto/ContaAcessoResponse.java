package com.curso.instrumento2026.api.dto;

import com.curso.instrumento2026.domain.PerfilAcesso;
import com.curso.instrumento2026.domain.StatusContaAcesso;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

public record ContaAcessoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long pessoaId,
        @Schema(example = "Ana Souza") String nomePessoa,
        @Schema(example = "operador.loja") String login,
        @Schema(example = "ATIVA") StatusContaAcesso status,
        @Schema(example = "[\"OPERADOR\"]") Set<PerfilAcesso> perfis) {
    public ContaAcessoResponse {
        perfis = Set.copyOf(perfis);
    }
}
