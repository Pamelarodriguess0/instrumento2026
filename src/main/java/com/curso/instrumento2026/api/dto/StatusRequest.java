package com.curso.instrumento2026.api.dto;

import com.curso.instrumento2026.domain.Status;
import jakarta.validation.constraints.NotNull;

    public record StatusRequest(
            @NotNull(message = "Status é obrigatório")
            Status status) {
    }
