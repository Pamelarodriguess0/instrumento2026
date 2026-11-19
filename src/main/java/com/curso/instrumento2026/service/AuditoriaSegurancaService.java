package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.ContaAcesso;
import com.curso.instrumento2026.domain.EventoSeguranca;
import com.curso.instrumento2026.domain.TipoEventoSeguranca;
import com.curso.instrumento2026.repository.ContaAcessoRepository;
import com.curso.instrumento2026.repository.EventoSegurancaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class AuditoriaSegurancaService {
    private final EventoSegurancaRepository eventos;
    private final ContaAcessoRepository contas;
    private final Clock clock;

    public AuditoriaSegurancaService(EventoSegurancaRepository eventos, ContaAcessoRepository contas, Clock clock) {
        this.eventos = eventos;
        this.contas = contas;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(TipoEventoSeguranca tipo, Long atorContaId, Long alvoContaId,
                          String identificadorHash, String enderecoOrigem) {
        eventos.save(new EventoSeguranca(tipo, clock.instant(), atorContaId, alvoContaId,
                identificadorHash, enderecoOrigem));
    }

    Long identificarConta(String login) {
        return contas.findByLoginIgnoreCase(login).map(ContaAcesso::getId).orElse(null);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public Page<EventoSeguranca> consultar(TipoEventoSeguranca tipo, Pageable pageable) {
        var pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "ocorridoEm", "id"));
        return tipo == null ? eventos.findAll(pagina) : eventos.findByTipo(tipo, pagina);
    }
}
