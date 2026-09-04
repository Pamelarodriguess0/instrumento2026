package com.curso.instrumento2026.api.dto;

import com.curso.instrumento2026.domain.Status;

public record FornecedorResponse(

        Long id,
        String razaoSocial,
        String cnpj,
        Status status

) {
}
