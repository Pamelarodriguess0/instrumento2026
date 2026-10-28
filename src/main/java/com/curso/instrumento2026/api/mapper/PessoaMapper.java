package com.curso.instrumento2026.api.mapper;

import com.curso.instrumento2026.api.dto.*;
import com.curso.instrumento2026.domain.Pessoa;
import org.springframework.stereotype.Component;

@Component
public class PessoaMapper {
    public Pessoa toEntity(PessoaRequest request) {
        return new Pessoa(request.nome(), request.email(), request.cpf(), java.time.LocalDate.now());
    }

    public PessoaResponse toResponse(Pessoa pessoa) {
        return new PessoaResponse(pessoa.getId(), pessoa.getNome(), pessoa.getEmail(), pessoa.getCpf(),
                pessoa.getDataCadastro(), pessoa.getStatus());
    }
}
