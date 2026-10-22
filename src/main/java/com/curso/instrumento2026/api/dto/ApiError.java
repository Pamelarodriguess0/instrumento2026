
package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(description = "Representação padronizada de erros da API")
public record ApiError(
        @Schema(description = "Data e hora em que ocorreu o erro")
        Instant timestamp,

        @Schema(description = "Código HTTP do erro", example = "404")
        int status,

        @Schema(description = "Descrição do status HTTP", example = "Not Found")
        String error,

        @Schema(description = "Mensagem explicativa do erro", example = "Instrumento não encontrado")
        String message,

        @Schema(description = "Caminho da requisição que gerou o erro", example = "/api/instrumentos/999")
        String path,

        @Schema(description = "Erros de validação associados aos campos da requisição")
        Map<String, String> fields
) {
}
