package com.curso.instrumento2026.api.mapper;

import com.curso.instrumento2026.api.dto.*;
import com.curso.instrumento2026.domain.Colaborador;
import org.springframework.stereotype.Component;

@Component
public class ColaboradorMapper {
    public ColaboradorResponse toResponse(Colaborador colaborador) {
        var pessoa = colaborador.getPessoa();
        return new ColaboradorResponse(colaborador.getId(), pessoa.getId(), pessoa.getNome(), pessoa.getEmail(),
                pessoa.getCpf(), colaborador.getMatricula(), colaborador.getFuncao(), colaborador.getDataAdmissao());
    }
}
