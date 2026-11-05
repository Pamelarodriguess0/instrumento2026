package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.ContaAcesso;
import com.curso.instrumento2026.domain.Pessoa;
import com.curso.instrumento2026.domain.PerfilAcesso;
import com.curso.instrumento2026.domain.Status;
import com.curso.instrumento2026.domain.StatusContaAcesso;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.ContaAcessoRepository;
import com.curso.instrumento2026.repository.PessoaRepository;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.LinkedHashSet;
import static com.curso.instrumento2026.service.ValidacaoCredenciais.normalizarLogin;
import static com.curso.instrumento2026.service.ValidacaoCredenciais.validarSenha;

@Service
public class ContaAcessoService {
    private final ContaAcessoRepository repository;
    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;

    public ContaAcessoService(ContaAcessoRepository repository, PessoaRepository pessoaRepository,
                              PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso cadastrar(Long pessoaId, String login, String senha) {
        if (pessoaId == null) throw new IllegalArgumentException("Pessoa é obrigatória");
        String loginNormalizado = normalizarLogin(login);
        validarSenha(senha);
        if (repository.existsByLoginIgnoreCase(loginNormalizado)) {
            throw new RecursoDuplicadoException("Login já cadastrado");
        }
        if (repository.existsByPessoaId(pessoaId)) {
            throw new RecursoDuplicadoException("Pessoa já possui conta de acesso");
        }
        Pessoa pessoa = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada"));
        if (pessoa.getStatus() != Status.ATIVO) {
            throw new IllegalArgumentException("Pessoa deve estar ativa");
        }
        ContaAcesso conta = new ContaAcesso(pessoa, loginNormalizado, passwordEncoder.encode(senha));
        try {
            // Faz o banco arbitrar também as disputas que ultrapassam as pré-consultas.
            return repository.saveAndFlush(conta);
        } catch (DataIntegrityViolationException exception) {
            for (Throwable causa = exception; causa != null; causa = causa.getCause()) {
                if (causa instanceof PSQLException violacao
                        && "23505".equals(violacao.getSQLState())
                        && violacao.getServerErrorMessage() != null) {
                    String constraint = violacao.getServerErrorMessage().getConstraint();
                    if ("uk_conta_acesso_login_lower".equals(constraint)) {
                        throw new RecursoDuplicadoException("Login já cadastrado");
                    }
                    if ("uk_conta_acesso_pessoa".equals(constraint)) {
                        throw new RecursoDuplicadoException("Pessoa já possui conta de acesso");
                    }
                }
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ContaAcesso buscarPorLogin(String login) {
        return repository.findByLoginIgnoreCase(normalizarLogin(login))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta de acesso não encontrada"));
    }

    @Transactional(readOnly = true)
    public ContaAcesso buscarPorId(Long id) {
        if (id == null) throw new IllegalArgumentException("Conta de acesso é obrigatória");
        return repository.findOneById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta de acesso não encontrada"));
    }

    @Transactional
    public void alterarSenha(String login, String senhaAtual, String novaSenha) {
        validarSenha(novaSenha);
        ContaAcesso conta = buscarPorLogin(login);
        if (senhaAtual == null || senhaAtual.isBlank()
                || senhaAtual.getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(senhaAtual, conta.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual inválida");
        }
        if (passwordEncoder.matches(novaSenha, conta.getSenhaHash())) {
            throw new IllegalArgumentException("Nova senha deve ser diferente da senha atual");
        }
        conta.alterarSenha(passwordEncoder.encode(novaSenha));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso alterarPerfis(Long id, Set<PerfilAcesso> novosPerfis) {
        if (novosPerfis == null || novosPerfis.isEmpty()) {
            throw new IllegalArgumentException("A conta deve possuir ao menos um perfil");
        }
        var validados = new LinkedHashSet<>(novosPerfis);
        if (validados.contains(null)) throw new IllegalArgumentException("Perfil é obrigatório");
        ContaAcesso conta = buscarPorId(id);
        boolean removeAdministrador = conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR)
                && !validados.contains(PerfilAcesso.ADMINISTRADOR);
        if (removeAdministrador) {
            if (propriaConta(conta)) {
                throw new IllegalArgumentException("Não é permitido remover o próprio perfil de administrador");
            }
            protegerUltimoAdministrador(conta);
        }
        conta.substituirPerfis(validados);
        return conta;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso alterarStatus(Long id, StatusContaAcesso status) {
        if (status == null) throw new IllegalArgumentException("Status é obrigatório");
        ContaAcesso conta = buscarPorId(id);
        if (status == StatusContaAcesso.BLOQUEADA) {
            if (propriaConta(conta)) {
                throw new IllegalArgumentException("Não é permitido bloquear a própria conta");
            }
            protegerUltimoAdministrador(conta);
        }
        if (status == StatusContaAcesso.ATIVA) conta.ativar(); else conta.bloquear();
        return conta;
    }

    private boolean propriaConta(ContaAcesso conta) {
        return conta.getLogin().equals(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private void protegerUltimoAdministrador(ContaAcesso conta) {
        if (conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR) && conta.podeAutenticar()
                && repository.contarAdministradoresAtivos() <= 1) {
            throw new IllegalArgumentException("O sistema deve manter ao menos um administrador ativo");
        }
    }

}
