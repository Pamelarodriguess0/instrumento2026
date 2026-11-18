package com.curso.instrumento2026.service;

import com.curso.instrumento2026.exception.CredenciaisInvalidasException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService {
    private final AuthenticationManager authenticationManager;
    private final SessaoAutenticacaoService sessaoService;

    public AutenticacaoService(AuthenticationManager authenticationManager, SessaoAutenticacaoService sessaoService) {
        this.authenticationManager = authenticationManager;
        this.sessaoService = sessaoService;
    }

    public TokenAcesso autenticar(String login, String senha) {
        return autenticarComSessao(login, senha).accessToken();
    }

    public TokensAutenticacao autenticarComSessao(String login, String senha) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(login, senha));
        } catch (AuthenticationException exception) {
            throw new CredenciaisInvalidasException();
        }
        return sessaoService.iniciar(authentication);
    }
}
