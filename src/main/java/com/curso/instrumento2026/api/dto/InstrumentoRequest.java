package com.curso.instrumento2026.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InstrumentoRequest(

        @NotBlank(message = "Código do instrumento é obrigatório")
        @Size(max = 50, message = "Código do instrumento deve possuir no máximo 50 caracteres")
        String codigoInstrumento,

        @NotBlank(message = "Nome do instrumento é obrigatório")
        @Size(max = 150, message = "Nome do instrumento deve possuir no máximo 150 caracteres")
        String nomeInstrumento,

        @PositiveOrZero(message = "Quantidade em estoque não pode ser negativa")
        int quantidadeEstoque,

        @NotNull(message = "Preço unitário é obrigatório")
        @PositiveOrZero(message = "Preço unitário não pode ser negativo")
        BigDecimal precoUnitario,

        @NotNull(message = "Estoque mínimo é obrigatório")
        @PositiveOrZero(message = "Estoque mínimo não pode ser negativo")
        BigDecimal estoqueMinimo,

        @NotNull(message = "Categoria é obrigatória")
        @Positive(message = "Identificador da categoria deve ser positivo")
        Long categoriaId,

        @Positive(message = "Identificador do fornecedor deve ser positivo")
        Long fornecedorId

) {
}