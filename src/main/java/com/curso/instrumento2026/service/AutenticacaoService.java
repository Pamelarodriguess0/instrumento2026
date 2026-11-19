package com.curso.instrumento2026.service;

import com.curso.instrumento2026.exception.CredenciaisInvalidasException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.curso.instrumento2026.domain.TipoEventoSeguranca.*;

@Service
@Transactional(noRollbackFor = CredenciaisInvalidasException.class)
public class AutenticacaoService {
    private final AuthenticationManager authenticationManager;
    private final SessaoAutenticacaoService sessaoService;
    private final ProtecaoAutenticacaoService protecaoService;
    private final AuditoriaSegurancaService auditoria;

    public AutenticacaoService(AuthenticationManager authenticationManager, SessaoAutenticacaoService sessaoService,
                               ProtecaoAutenticacaoService protecaoService, AuditoriaSegurancaService auditoria) {
        this.authenticationManager = authenticationManager;
        this.sessaoService = sessaoService;
        this.protecaoService = protecaoService;
        this.auditoria = auditoria;
    }

    public TokenAcesso autenticar(String login, String senha) {
        return autenticarComSessao(login, senha).accessToken();
    }

    public TokensAutenticacao autenticarComSessao(String login, String senha) {
        return autenticarComSessao(login, senha, null);
    }

    public TokensAutenticacao autenticarComSessao(String login, String senha, String enderecoOrigem) {
        var protecao = protecaoService.bloquearParaTentativa(login);
        String chave = protecao.getChaveLogin();
        if (protecaoService.estaBloqueada(protecao)) {
            auditoria.registrar(LOGIN_RECUSADO_BLOQUEIO, null, null, chave, enderecoOrigem);
            throw new CredenciaisInvalidasException();
        }

        Authentication authentication;
        TokensAutenticacao tokens;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(login, senha));
            tokens = sessaoService.iniciar(authentication);
        } catch (AuthenticationException | CredenciaisInvalidasException exception) {
            boolean ativouBloqueio = protecaoService.registrarFalha(protecao);
            auditoria.registrar(LOGIN_FALHA, null, null, chave, enderecoOrigem);
            if (ativouBloqueio) {
                auditoria.registrar(BLOQUEIO_TEMPORARIO_ATIVADO, null, null, chave, enderecoOrigem);
            }
            throw new CredenciaisInvalidasException();
        }
        protecao.registrarSucesso();
        Long contaId = auditoria.identificarConta(authentication.getName());
        auditoria.registrar(LOGIN_SUCESSO, contaId, contaId, chave, enderecoOrigem);
        return tokens;
    }
}
