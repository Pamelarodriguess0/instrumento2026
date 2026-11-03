package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.ContaAcesso;
import com.curso.instrumento2026.domain.Pessoa;
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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

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
    public ContaAcesso alterarStatus(Long id, StatusContaAcesso status) {
        if (status == null) throw new IllegalArgumentException("Status é obrigatório");
        ContaAcesso conta = buscarPorId(id);
        if (status == StatusContaAcesso.ATIVA) conta.ativar(); else conta.bloquear();
        return conta;
    }

    private static String normalizarLogin(String login) {
        if (login == null || login.isBlank()) throw new IllegalArgumentException("Login é obrigatório");
        String normalizado = login.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() < 3 || normalizado.length() > 80) {
            throw new IllegalArgumentException("Login deve possuir entre 3 e 80 caracteres");
        }
        if (!normalizado.matches("^[a-z0-9._-]+$")) {
            throw new IllegalArgumentException("Login aceita somente letras, números, ponto, hífen e sublinhado");
        }
        return normalizado;
    }

    private static void validarSenha(String senha) {
        if (senha == null || senha.isBlank()) throw new IllegalArgumentException("Senha é obrigatória");
        if (senha.codePointCount(0, senha.length()) < 12) {
            throw new IllegalArgumentException("Senha deve possuir ao menos 12 caracteres");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Senha deve possuir no máximo 72 bytes em UTF-8");
        }
    }
}
