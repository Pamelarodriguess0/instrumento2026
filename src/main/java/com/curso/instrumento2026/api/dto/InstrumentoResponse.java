
package com.curso.instrumento2026.api.dto;

import com.curso.instrumento2026.domain.Status;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados de um instrumento musical retornados pela API")
public record InstrumentoResponse(

        @Schema(description = "Identificador do instrumento", example = "1")
        Long id,

        @Schema(description = "Código único do instrumento", example = "INT001")
        String codigoInstrumento,

        @Schema(description = "Nome do instrumento", example = "Violino Eagle")
        String nomeInstrumento,

        @Schema(description = "Quantidade disponível em estoque", example = "10")
        int quantidadeEstoque,

        @Schema(description = "Preço unitário do instrumento", example = "1800.00")
        BigDecimal precoUnitario,

        @Schema(description = "Estoque mínimo do instrumento", example = "2.000")
        BigDecimal estoqueMinimo,

        @Schema(description = "Valor total dos instrumentos em estoque", example = "18000.00")
        BigDecimal valorEstoque,

        @Schema(description = "Data de cadastro do instrumento", example = "2026-10-20")
        LocalDate dataCadastro,

        @Schema(description = "Status atual do instrumento", example = "ATIVO")
        Status status,

        @Schema(description = "Identificador da categoria", example = "1")
        Long categoriaId,

        @Schema(description = "Nome da categoria", example = "Cordas")
        String categoriaNome,

        @Schema(description = "Identificador do fornecedor, quando vinculado", example = "1", nullable = true)
        Long fornecedorId,

        @Schema(description = "Razão social do fornecedor, quando vinculado", example = "Instrumentos Musicais Brasil Ltda", nullable = true)
        String fornecedorRazaoSocial

) {
}
