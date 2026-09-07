package com.curso.instrumento2026.api.mapper;

import com.curso.instrumento2026.api.dto.InstrumentoRequest;
import com.curso.instrumento2026.api.dto.InstrumentoResponse;
import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.domain.Fornecedor;
import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.domain.Status;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class InstrumentoMapper {

    public Instrumento toEntity(InstrumentoRequest request) {

        return new Instrumento(
                request.codigoInstrumento(),
                request.nomeInstrumento(),
                request.quantidadeEstoque(),
                request.precoUnitario(),
                request.estoqueMinimo(),
                LocalDate.now()
        );
    }

    public InstrumentoResponse toResponse(Instrumento instrumento) {

        Fornecedor fornecedor = instrumento.getFornecedor();

        return new InstrumentoResponse(
                instrumento.getId(),
                instrumento.getCodigoInstrumento(),
                instrumento.getNomeInstrumento(),
                instrumento.getQuantidadeEstoque(),
                instrumento.getPrecoUnitario(),
                instrumento.getEstoqueMinimo(),
                instrumento.calcularValorEstoque(),
                instrumento.getDataCadastro(),
                instrumento.getStatus(),
                instrumento.getCategoria().getId(),
                instrumento.getCategoria().getNome(),
                fornecedor != null ? fornecedor.getId() : null,
                fornecedor != null ? fornecedor.getRazaoSocial() : null
        );
    }
}