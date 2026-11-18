package com.curso.instrumento2026.service;

import com.curso.instrumento2026.config.RefreshTokenProperties;
import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.exception.RefreshTokenInvalidoException;
import com.curso.instrumento2026.repository.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class SessaoAutenticacaoService {
    private final SessaoRefreshTokenRepository sessoes;
    private final ContaAcessoRepository contas;
    private final TokenService tokens;
    private final RefreshTokenProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public SessaoAutenticacaoService(SessaoRefreshTokenRepository sessoes, ContaAcessoRepository contas,
                                    TokenService tokens, RefreshTokenProperties properties, Clock clock) {
        this.sessoes = sessoes;
        this.contas = contas;
        this.tokens = tokens;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public TokensAutenticacao iniciar(Authentication authentication) {
        var conta = contas.findByLoginIgnoreCase(authentication.getName())
                .orElseThrow(RefreshTokenInvalidoException::new);
        if (!conta.podeAutenticar()) throw new RefreshTokenInvalidoException();
        Instant agora = clock.instant();
        return emitirPar(conta, UUID.randomUUID().toString(), agora,
                agora.plus(properties.absoluteExpiration()));
    }

    @Transactional(noRollbackFor = RefreshTokenInvalidoException.class)
    public TokensAutenticacao renovar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) throw new RefreshTokenInvalidoException();
        var sessao = sessoes.buscarPorHashParaAtualizacao(calcularHash(refreshToken))
                .orElseThrow(RefreshTokenInvalidoException::new);
        Instant agora = clock.instant();
        if (sessao.foiRevogada()) throw new RefreshTokenInvalidoException();
        if (sessao.foiUtilizada()) rejeitarFamilia(sessao, agora, MotivoRevogacaoRefreshToken.REUTILIZACAO);
        if (sessao.expirouAbsolutamente(agora))
            rejeitarFamilia(sessao, agora, MotivoRevogacaoRefreshToken.EXPIRACAO_ABSOLUTA);
        if (sessao.expirouPorInatividade(agora, properties.idleTimeout()))
            rejeitarFamilia(sessao, agora, MotivoRevogacaoRefreshToken.INATIVIDADE);
        if (!sessao.getConta().podeAutenticar())
            rejeitarFamilia(sessao, agora, MotivoRevogacaoRefreshToken.CONTA_INATIVA);
        sessao.marcarUtilizada(agora);
        return emitirPar(sessao.getConta(), sessao.getFamiliaId(), agora, sessao.getExpiraEm());
    }

    private void rejeitarFamilia(SessaoRefreshToken sessao, Instant agora, MotivoRevogacaoRefreshToken motivo) {
        sessoes.revogarFamilia(sessao.getFamiliaId(), agora, motivo);
        throw new RefreshTokenInvalidoException();
    }

    @Transactional
    public void encerrar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        sessoes.buscarPorHashParaAtualizacao(calcularHash(refreshToken)).ifPresent(sessao ->
                sessoes.revogarFamilia(sessao.getFamiliaId(), clock.instant(), MotivoRevogacaoRefreshToken.LOGOUT));
    }

    @Transactional
    public void encerrarTodas(String login) {
        if (login == null || login.isBlank())
            throw new com.curso.instrumento2026.exception.CredenciaisInvalidasException();
        var conta = contas.findByLoginIgnoreCase(login)
                .orElseThrow(com.curso.instrumento2026.exception.CredenciaisInvalidasException::new);
        if (!conta.podeAutenticar())
            throw new com.curso.instrumento2026.exception.CredenciaisInvalidasException();
        sessoes.revogarTodasDaConta(conta.getId(), clock.instant(), MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL);
    }

    private TokensAutenticacao emitirPar(ContaAcesso conta, String familia, Instant agora, Instant limite) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var authorities = conta.getPerfis().stream()
                .map(perfil -> new SimpleGrantedAuthority("ROLE_" + perfil.name())).toList();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(conta.getLogin(), null, authorities);
        // Depois das validações, nenhuma rejeição prevista deve ser lançada: demais falhas fazem rollback.
        var access = tokens.emitir(authentication, familia);
        sessoes.save(new SessaoRefreshToken(conta, familia, calcularHash(refresh), agora, agora, limite));
        return new TokensAutenticacao(access, refresh, limite);
    }

    private static String calcularHash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
