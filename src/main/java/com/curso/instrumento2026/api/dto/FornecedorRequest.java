
package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para cadastrar um fornecedor de instrumentos musicais")
public record FornecedorRequest(

        @Schema(
                description = "Razão social do fornecedor",
                example = "Instrumentos Musicais Brasil Ltda",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 150, message = "Razão social deve possuir no máximo 150 caracteres")
        String razaoSocial,

        @Schema(
                description = "CNPJ do fornecedor, composto por 14 caracteres",
                example = "12345678000195",
                minLength = 14,
                maxLength = 14,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "CNPJ é obrigatório")
        @Size(min = 14, max = 14, message = "CNPJ deve possuir 14 caracteres")
        String cnpj

) {
}
