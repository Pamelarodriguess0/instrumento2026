package com.curso.instrumento2026.api.dto;

import com.curso.instrumento2026.domain.Status;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InstrumentoResponse(

        Long id,
        String codigoInstrumento,
        String nomeInstrumento,
        int quantidadeEstoque,
        BigDecimal precoUnitario,
        BigDecimal estoqueMinimo,
        BigDecimal valorEstoque,
        LocalDate dataCadastro,
        Status status,
        Long categoriaId,
        String categoriaNome,
        Long fornecedorId,
        String fornecedorRazaoSocial

) {
}