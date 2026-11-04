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
    private final TokenService tokenService;

    public AutenticacaoService(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    public TokenAcesso autenticar(String login, String senha) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(login, senha));
        } catch (AuthenticationException exception) {
            throw new CredenciaisInvalidasException();
        }
        return tokenService.emitir(authentication);
    }
}
