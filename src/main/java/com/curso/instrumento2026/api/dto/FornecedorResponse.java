
package com.curso.instrumento2026.api.dto;

import com.curso.instrumento2026.domain.Status;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de um fornecedor retornados pela API")
public record FornecedorResponse(

        @Schema(
                description = "Identificador do fornecedor",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Razão social do fornecedor",
                example = "Instrumentos Musicais Brasil Ltda"
        )
        String razaoSocial,

        @Schema(
                description = "CNPJ do fornecedor",
                example = "12345678000195"
        )
        String cnpj,

        @Schema(
                description = "Status atual do fornecedor",
                example = "ATIVO"
        )
        Status status

) {
}
