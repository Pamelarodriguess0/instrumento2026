package com.curso.instrumento2026.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaInstrumentoRequest(

        @NotBlank(message = "Nome da categoria é obrigatório")
        @Size(max = 120, message = "Nome da categoria deve possuir no máximo 120 caracteres")
        String nome

) {
}
