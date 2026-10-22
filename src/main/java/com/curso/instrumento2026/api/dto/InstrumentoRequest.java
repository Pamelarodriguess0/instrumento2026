
package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados necessários para cadastrar um instrumento musical")
public record InstrumentoRequest(

        @Schema(
                description = "Código único do instrumento",
                example = "INT001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Código do instrumento é obrigatório")
        @Size(max = 50, message = "Código do instrumento deve possuir no máximo 50 caracteres")
        String codigoInstrumento,

        @Schema(
                description = "Nome do instrumento musical",
                example = "Violino",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Nome do instrumento é obrigatório")
        @Size(max = 150, message = "Nome do instrumento deve possuir no máximo 150 caracteres")
        String nomeInstrumento,

        @Schema(
                description = "Quantidade inicial disponível em estoque",
                example = "10",
                minimum = "0"
        )
        @PositiveOrZero(message = "Quantidade em estoque não pode ser negativa")
        int quantidadeEstoque,

        @Schema(
                description = "Preço unitário do instrumento",
                example = "1500.00",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Preço unitário é obrigatório")
        @PositiveOrZero(message = "Preço unitário não pode ser negativo")
        BigDecimal precoUnitario,

        @Schema(
                description = "Quantidade mínima desejada em estoque",
                example = "2.000",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Estoque mínimo é obrigatório")
        @PositiveOrZero(message = "Estoque mínimo não pode ser negativo")
        BigDecimal estoqueMinimo,

        @Schema(
                description = "Identificador da categoria do instrumento",
                example = "1",
                minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Categoria é obrigatória")
        @Positive(message = "Identificador da categoria deve ser positivo")
        Long categoriaId,

        @Schema(
                description = "Identificador do fornecedor, quando informado",
                example = "1",
                minimum = "1",
                nullable = true
        )
        @Positive(message = "Identificador do fornecedor deve ser positivo")
        Long fornecedorId

) {
}
