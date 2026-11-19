package com.curso.instrumento2026.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "evento_seguranca")
public class EventoSeguranca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoEventoSeguranca tipo;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(name = "ator_conta_id")
    private Long atorContaId;

    @Column(name = "alvo_conta_id")
    private Long alvoContaId;

    @Column(name = "identificador_hash", length = 64)
    private String identificadorHash;

    @Column(name = "endereco_origem", length = 45)
    private String enderecoOrigem;

    protected EventoSeguranca() { }

    public EventoSeguranca(TipoEventoSeguranca tipo, Instant ocorridoEm, Long atorContaId, Long alvoContaId,
                           String identificadorHash, String enderecoOrigem) {
        if (identificadorHash != null && !identificadorHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Identificador de auditoria inválido");
        }
        if (enderecoOrigem != null && (enderecoOrigem.length() > 45
                || enderecoOrigem.contains("\r") || enderecoOrigem.contains("\n"))) {
            throw new IllegalArgumentException("Endereço de origem inválido");
        }
        this.tipo = Objects.requireNonNull(tipo);
        this.ocorridoEm = Objects.requireNonNull(ocorridoEm);
        this.atorContaId = atorContaId;
        this.alvoContaId = alvoContaId;
        this.identificadorHash = identificadorHash;
        this.enderecoOrigem = enderecoOrigem;
    }

    public Long getId() { return id; }
    public TipoEventoSeguranca getTipo() { return tipo; }
    public Instant getOcorridoEm() { return ocorridoEm; }
    public Long getAtorContaId() { return atorContaId; }
    public Long getAlvoContaId() { return alvoContaId; }
    public String getEnderecoOrigem() { return enderecoOrigem; }
}
