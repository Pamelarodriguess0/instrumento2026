package com.curso.instrumento2026.domain;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "protecao_autenticacao")
public class ProtecaoAutenticacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_login", nullable = false, unique = true, length = 64)
    private String chaveLogin;

    @Column(name = "falhas_consecutivas", nullable = false)
    private int falhasConsecutivas;

    @Column(name = "janela_iniciada_em")
    private Instant janelaIniciadaEm;

    @Column(name = "bloqueada_ate")
    private Instant bloqueadaAte;

    protected ProtecaoAutenticacao() { }

    public ProtecaoAutenticacao(String chaveLogin) {
        if (chaveLogin == null || !chaveLogin.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Chave do login deve ser um hash SHA-256 hexadecimal");
        }
        this.chaveLogin = chaveLogin;
    }

    public boolean registrarFalha(Instant agora, int maximoTentativas, Duration janela, Duration duracaoBloqueio) {
        if (janelaIniciadaEm == null || !agora.isBefore(janelaIniciadaEm.plus(janela))) {
            falhasConsecutivas = 0;
            janelaIniciadaEm = agora;
        }
        falhasConsecutivas++;
        if (falhasConsecutivas >= maximoTentativas) {
            bloqueadaAte = agora.plus(duracaoBloqueio);
            return true;
        }
        return false;
    }

    public boolean estaBloqueada(Instant agora) {
        return bloqueadaAte != null && agora.isBefore(bloqueadaAte);
    }

    public void registrarSucesso() {
        falhasConsecutivas = 0;
        janelaIniciadaEm = null;
        bloqueadaAte = null;
    }

    public Long getId() { return id; }
    public String getChaveLogin() { return chaveLogin; }
    public int getFalhasConsecutivas() { return falhasConsecutivas; }
    public Instant getJanelaIniciadaEm() { return janelaIniciadaEm; }
    public Instant getBloqueadaAte() { return bloqueadaAte; }
}
