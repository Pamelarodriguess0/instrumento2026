package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.ColaboradorRepository;
import com.curso.instrumento2026.repository.PessoaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

@Service
public class ColaboradorService {
    private final ColaboradorRepository repository;
    private final PessoaRepository pessoaRepository;

    public ColaboradorService(ColaboradorRepository repository, PessoaRepository pessoaRepository) {
        this.repository = repository;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional
    public Colaborador cadastrar(Long pessoaId, String matricula, FuncaoColaborador funcao, LocalDate dataAdmissao) {
        Pessoa pessoa = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada"));
        if (pessoa.getStatus() != Status.ATIVO) throw new IllegalArgumentException("Pessoa deve estar ativa");
        if (repository.existsByPessoaId(pessoaId)) {
            throw new RecursoDuplicadoException("Pessoa já possui cadastro de colaborador");
        }
        Colaborador colaborador = new Colaborador(pessoa, matricula, funcao, dataAdmissao);
        validarMatricula(colaborador.getMatricula(), null);
        return repository.save(colaborador);
    }

    @Transactional
    public Colaborador alterar(Long id, String matricula, FuncaoColaborador funcao) {
        Colaborador colaborador = buscarEntidade(id);
        Colaborador dados = new Colaborador(colaborador.getPessoa(), matricula, funcao, colaborador.getDataAdmissao());
        validarMatricula(dados.getMatricula(), id);
        colaborador.alterarDados(dados.getMatricula(), dados.getFuncao());
        return colaborador;
    }

    @Transactional(readOnly = true)
    public Colaborador buscarPorId(Long id) {
        return buscarEntidade(id);
    }

    @Transactional(readOnly = true)
    public Page<Colaborador> pesquisar(String nome, String matricula, FuncaoColaborador funcao, Pageable pageable) {
        pageable.getSort().forEach(ordem -> {
            if (!Set.of("id", "matricula", "funcao", "dataAdmissao").contains(ordem.getProperty())) {
                throw new IllegalArgumentException("Campo de ordenação inválido: " + ordem.getProperty());
            }
        });
        Specification<Colaborador> filtros = (root, query, cb) -> cb.conjunction();
        if (nome != null && !nome.isBlank()) {
            String trecho = "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";
            filtros = filtros.and((root, query, cb) -> cb.like(cb.lower(root.get("pessoa").get("nome")), trecho));
        }
        if (matricula != null && !matricula.isBlank()) {
            filtros = filtros.and((root, query, cb) -> cb.equal(root.get("matricula"), matricula.trim()));
        }
        if (funcao != null) filtros = filtros.and((root, query, cb) -> cb.equal(root.get("funcao"), funcao));
        return repository.findAll(filtros, pageable);
    }

    private Colaborador buscarEntidade(Long id) {
        return repository.findOneById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Colaborador não encontrado"));
    }

    private void validarMatricula(String matricula, Long idAtual) {
        boolean duplicada = idAtual == null ? repository.existsByMatricula(matricula)
                : repository.existsByMatriculaAndIdNot(matricula, idAtual);
        if (duplicada) throw new RecursoDuplicadoException("Matrícula já cadastrada");
    }
}
