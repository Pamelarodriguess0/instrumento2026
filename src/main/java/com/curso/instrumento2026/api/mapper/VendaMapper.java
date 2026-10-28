package com.curso.instrumento2026.api.mapper;

import com.curso.instrumento2026.api.dto.*;
import com.curso.instrumento2026.domain.Venda;
import com.curso.instrumento2026.domain.ItemVenda;
import org.springframework.stereotype.Component;

@Component
public class VendaMapper {
    public VendaResponse toResponse(Venda venda) {
        var itens = venda.getItens().stream().map(this::toItemResponse).toList();
        return new VendaResponse(venda.getId(), venda.getCliente().getId(), venda.getCliente().getPessoa().getNome(),
                venda.getVendedor().getId(), venda.getVendedor().getPessoa().getNome(), venda.getDataVenda(),
                venda.getStatus(), itens, venda.calcularTotal());
    }

    public VendaResumoResponse toResumo(Venda venda) {
        return new VendaResumoResponse(venda.getId(), venda.getCliente().getId(), venda.getCliente().getPessoa().getNome(),
                venda.getVendedor().getId(), venda.getVendedor().getPessoa().getNome(), venda.getDataVenda(), venda.getStatus());
    }

    private ItemVendaResponse toItemResponse(ItemVenda item) {
        return new ItemVendaResponse(item.getId(), item.getInstrumento().getId(), item.getInstrumento().getNomeInstrumento(),
                item.getQuantidade(), item.getValorUnitario(), item.calcularSubtotal());
    }
}
