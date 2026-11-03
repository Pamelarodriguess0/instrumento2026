package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.ContaAcesso;
import com.curso.instrumento2026.domain.PerfilAcesso;
import com.curso.instrumento2026.domain.Pessoa;
import com.curso.instrumento2026.repository.ContaAcessoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContaAcessoUserDetailsServiceTest {
    private final ContaAcessoRepository repository = mock(ContaAcessoRepository.class);
    private final ContaAcessoUserDetailsService service = new ContaAcessoUserDetailsService(repository);

    @Test
    void deveNormalizarLoginEConverterSomentePerfisDeAcessoEmAuthorities() {
        Pessoa pessoa = new Pessoa("Pessoa sem papel comercial", "authorities@example.com", "52998224725", LocalDate.now());
        ContaAcesso conta = new ContaAcesso(pessoa, "operador.loja", "hash-persistido");
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        when(repository.findByLoginIgnoreCase("operador.loja")).thenReturn(Optional.of(conta));

        var usuario = service.loadUserByUsername("  OPERADOR.LOJA  ");

        assertEquals("operador.loja", usuario.getUsername());
        assertEquals(conta.getSenhaHash(), usuario.getPassword());
        assertTrue(usuario.isEnabled());
        assertEquals(Set.of("ROLE_OPERADOR", "ROLE_ADMINISTRADOR"), usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
        assertFalse(usuario.toString().contains(conta.getSenhaHash()));
        verify(repository).findByLoginIgnoreCase("operador.loja");
    }

    @Test
    void loginInexistenteDeveGerarErroGenericoSemExporIdentificacao() {
        when(repository.findByLoginIgnoreCase("ausente")).thenReturn(Optional.empty());
        var erro = assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ausente"));
        assertEquals("Login ou senha inválidos", erro.getMessage());
    }

    @Test
    void loginVazioOuNuloDeveGerarMesmoErroSemConsultarBanco() {
        for (String login : new String[]{null, "", "   "}) {
            var erro = assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(login));
            assertEquals("Login ou senha inválidos", erro.getMessage());
        }
        verifyNoInteractions(repository);
    }
}
