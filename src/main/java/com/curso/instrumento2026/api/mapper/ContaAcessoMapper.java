package com.curso.instrumento2026.api.mapper;

import com.curso.instrumento2026.api.dto.ContaAcessoResponse;
import com.curso.instrumento2026.domain.ContaAcesso;
import org.springframework.stereotype.Component;

@Component
public class ContaAcessoMapper {
    public ContaAcessoResponse toResponse(ContaAcesso conta) {
        return new ContaAcessoResponse(conta.getId(), conta.getPessoa().getId(), conta.getPessoa().getNome(),
                conta.getLogin(), conta.getStatus(), conta.getPerfis());
    }
}
