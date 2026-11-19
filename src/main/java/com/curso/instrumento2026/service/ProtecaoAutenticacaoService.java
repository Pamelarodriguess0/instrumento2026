package com.curso.instrumento2026.service;

import com.curso.instrumento2026.config.ProtecaoAutenticacaoProperties;
import com.curso.instrumento2026.domain.ProtecaoAutenticacao;
import com.curso.instrumento2026.repository.ProtecaoAutenticacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class ProtecaoAutenticacaoService {
    private final ProtecaoAutenticacaoRepository repository;
    private final ProtecaoAutenticacaoProperties properties;
    private final Clock clock;

    public ProtecaoAutenticacaoService(ProtecaoAutenticacaoRepository repository,
                                      ProtecaoAutenticacaoProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ProtecaoAutenticacao bloquearParaTentativa(String login) {
        String chave = calcularChave(login);
        repository.inserirSeAusente(chave);
        return repository.buscarParaAtualizacao(chave).orElseThrow();
    }

    boolean estaBloqueada(ProtecaoAutenticacao protecao) {
        return protecao.estaBloqueada(clock.instant());
    }

    boolean registrarFalha(ProtecaoAutenticacao protecao) {
        return protecao.registrarFalha(clock.instant(), properties.maxFailedAttempts(),
                properties.failureWindow(), properties.lockDuration());
    }

    private static String calcularChave(String login) {
        String normalizado = login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(normalizado.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
