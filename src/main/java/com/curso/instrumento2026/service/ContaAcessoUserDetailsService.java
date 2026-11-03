package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.ContaAcesso;
import com.curso.instrumento2026.repository.ContaAcessoRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class ContaAcessoUserDetailsService implements UserDetailsService {
    private final ContaAcessoRepository repository;

    public ContaAcessoUserDetailsService(ContaAcessoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) {
        if (login == null || login.isBlank()) {
            throw new UsernameNotFoundException("Login ou senha inválidos");
        }
        ContaAcesso conta = repository.findByLoginIgnoreCase(login.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Login ou senha inválidos"));
        String[] autoridades = conta.getPerfis().stream()
                .map(perfil -> "ROLE_" + perfil.name())
                .toArray(String[]::new);

        return User.withUsername(conta.getLogin())
                .password(conta.getSenhaHash())
                .authorities(autoridades)
                .disabled(!conta.podeAutenticar())
                .build();
    }
}
