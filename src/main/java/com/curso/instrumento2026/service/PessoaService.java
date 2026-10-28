package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.Pessoa;
import com.curso.instrumento2026.domain.Status;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.PessoaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.Set;
import java.util.Objects;

@Service
public class PessoaService {
    private final PessoaRepository repository;

    public PessoaService(PessoaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Pessoa cadastrar(Pessoa pessoa) {
        validarUnicidade(pessoa.getCpf(), pessoa.getEmail(), null);
        return repository.save(pessoa);
    }

    @Transactional
    public Pessoa alterar(Long id, String nome, String email, String cpf) {
        Pessoa pessoa = buscarEntidade(id);
        // Reutiliza as validações e a normalização antes da consulta de unicidade.
        Pessoa dados = new Pessoa(nome, email, cpf, pessoa.getDataCadastro());
        validarUnicidade(dados.getCpf(), dados.getEmail(), id);
        pessoa.alterarDados(dados.getNome(), dados.getEmail(), dados.getCpf());
        return pessoa;
    }

    @Transactional
    public Pessoa alterarStatus(Long id, Status status) {
        Pessoa pessoa = buscarEntidade(id);
        Objects.requireNonNull(status, "Status é obrigatório");
        if (status == Status.ATIVO) pessoa.ativar(); else pessoa.inativar();
        return pessoa;
    }

    @Transactional(readOnly = true)
    public Pessoa buscarPorId(Long id) {
        return buscarEntidade(id);
    }

    @Transactional(readOnly = true)
    public Page<Pessoa> pesquisar(String nome, String cpf, Status status, Pageable pageable) {
        pageable.getSort().forEach(ordem -> {
            if (!Set.of("id", "nome", "email", "cpf", "dataCadastro", "status").contains(ordem.getProperty())) {
                throw new IllegalArgumentException("Campo de ordenação inválido: " + ordem.getProperty());
            }
        });
        Specification<Pessoa> filtros = (root, query, cb) -> cb.conjunction();
        if (nome != null && !nome.isBlank()) {
            String trecho = "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";
            filtros = filtros.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), trecho));
        }
        if (cpf != null && !cpf.isBlank()) {
            filtros = filtros.and((root, query, cb) -> cb.equal(root.get("cpf"), cpf.trim()));
        }
        if (status != null) {
            filtros = filtros.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return repository.findAll(filtros, pageable);
    }

    private Pessoa buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada"));
    }

    private void validarUnicidade(String cpf, String email, Long idAtual) {
        boolean cpfDuplicado = idAtual == null ? repository.existsByCpf(cpf)
                : repository.existsByCpfAndIdNot(cpf, idAtual);
        boolean emailDuplicado = idAtual == null ? repository.existsByEmailIgnoreCase(email)
                : repository.existsByEmailIgnoreCaseAndIdNot(email, idAtual);
        if (cpfDuplicado) throw new RecursoDuplicadoException("CPF já cadastrado");
        if (emailDuplicado) throw new RecursoDuplicadoException("E-mail já cadastrado");
    }
}
