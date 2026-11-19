package com.curso.instrumento2026.api.dto;

import com.curso.instrumento2026.domain.EventoSeguranca;
import com.curso.instrumento2026.domain.TipoEventoSeguranca;

import java.time.Instant;

public record EventoSegurancaResponse(Long id, TipoEventoSeguranca tipo, Instant ocorridoEm,
                                     Long atorContaId, Long alvoContaId, String enderecoOrigem) {
    public static EventoSegurancaResponse de(EventoSeguranca evento) {
        return new EventoSegurancaResponse(evento.getId(), evento.getTipo(), evento.getOcorridoEm(),
                evento.getAtorContaId(), evento.getAlvoContaId(), evento.getEnderecoOrigem());
    }
}
