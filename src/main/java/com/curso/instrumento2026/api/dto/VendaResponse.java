package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.curso.instrumento2026.domain.StatusVenda;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Detalhe da venda com itens, preços históricos e total calculado")
public record VendaResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long clienteId,
        @Schema(example = "Ana Souza") String clienteNome,
        @Schema(example = "2") Long vendedorId,
        @Schema(example = "Bruno Lima") String vendedorNome,
        @Schema(example = "2026-10-28T10:30:00") LocalDateTime dataVenda,
        @Schema(example = "ABERTA") StatusVenda status,
        @Schema(description = "Itens com preço histórico capturado na inclusão") List<ItemVendaResponse> itens,
        @Schema(example = "3000.00") BigDecimal total) {
}
