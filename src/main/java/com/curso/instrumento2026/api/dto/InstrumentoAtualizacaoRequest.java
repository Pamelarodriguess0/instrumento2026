package com.curso.instrumento2026.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InstrumentoAtualizacaoRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150,
                message = "Nome deve possuir no máximo 150 caracteres")
        String nomeInstrumento,

        @NotNull(message = "Preço unitário é obrigatório")
        @Positive(message = "Preço unitário deve ser maior que zero")
        BigDecimal precoUnitario,

        @NotNull(message = "Estoque mínimo é obrigatório")
        @PositiveOrZero(message = "Estoque mínimo não pode ser negativo")
        BigDecimal estoqueMinimo,

        @NotNull(message = "Categoria é obrigatória")
        @Positive(message = "Identificador da categoria deve ser positivo")
        Long categoriaId,

        @Positive(message = "Identificador do fornecedor deve ser positivo")
        Long fornecedorId ) {
}

