package com.curso.instrumento2026.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FornecedorRequest(

        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 150, message = "Razão social deve possuir no máximo 150 caracteres")
        String razaoSocial,

        @NotBlank(message = "CNPJ é obrigatório")
        @Size(min = 14, max = 14, message = "CNPJ deve possuir 14 caracteres")
        String cnpj

) {
}