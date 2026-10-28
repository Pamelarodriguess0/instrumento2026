package com.curso.instrumento2026.api.mapper;

import com.curso.instrumento2026.api.dto.*;
import com.curso.instrumento2026.domain.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {
    public ClienteResponse toResponse(Cliente cliente) {
        var pessoa = cliente.getPessoa();
        return new ClienteResponse(cliente.getId(), pessoa.getId(), pessoa.getNome(), pessoa.getEmail(),
                pessoa.getCpf(), cliente.getTelefone());
    }
}
