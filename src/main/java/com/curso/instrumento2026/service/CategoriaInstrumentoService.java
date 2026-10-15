package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.domain.Status;
import com.curso.instrumento2026.exception.RecursoEmUsoException;
import com.curso.instrumento2026.repository.InstrumentoRepository;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.CategoriaInstrumentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@Service
public class CategoriaInstrumentoService {
    @Transactional(readOnly = true)
    public Page<CategoriaInstrumento> pesquisar(String nome, Status status, Pageable pageable) {
        validarOrdenacao(pageable, Set.of("id", "nome", "status"));
        Specification<CategoriaInstrumento> filtros = (root, query, cb) -> cb.conjunction();
        if (nome != null && !nome.isBlank()) {
            String trecho = "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";
            filtros = filtros.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), trecho));
        }
        if (status != null) {
            filtros = filtros.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return repository.findAll(filtros, pageable);
    }

    private void validarOrdenacao(Pageable pageable, Set<String> camposPermitidos) {
        pageable.getSort().forEach(ordem -> {
            if (!camposPermitidos.contains(ordem.getProperty())) {
                throw new IllegalArgumentException("Campo de ordenação inválido: " + ordem.getProperty());
            }
        });
    }

    private final CategoriaInstrumentoRepository repository;
    private final InstrumentoRepository instrumentoRepository;

    public CategoriaInstrumentoService(CategoriaInstrumentoRepository repository,
                                       InstrumentoRepository instrumentoRepository) {
        this.repository = repository;
        this.instrumentoRepository = instrumentoRepository;
    }

    @Transactional
    public CategoriaInstrumento alterar(Long id, String nome) {
        String nomeNormalizado = nome == null ? null : nome.trim();
        if (repository.existsByNomeIgnoreCaseAndIdNot(nomeNormalizado, id)) {
            throw new RecursoDuplicadoException("Nome da categoria já cadastrado");
        }
        CategoriaInstrumento categoria = buscarPorId(id);
        categoria.alterarNome(nomeNormalizado);
        return categoria;
    }

    @Transactional
    public CategoriaInstrumento alterarStatus(Long id, Status status) {
        CategoriaInstrumento categoria = buscarPorId(id);
        if (status == Status.ATIVO) {
            categoria.ativar();
        } else {
            categoria.inativar();
        }
        return categoria;
    }

    @Transactional
    public void excluir(Long id) {
        CategoriaInstrumento categoria = buscarPorId(id);
        if (instrumentoRepository.existsByCategoriaId(id)) {
            throw new RecursoEmUsoException(
                    "Categoria não pode ser excluída porque possui instrumentos");
        }
        repository.delete(categoria);
        repository.flush();
    }

    @Transactional
    public CategoriaInstrumento cadastrar(CategoriaInstrumento categoria){
        if (repository.existsByNomeIgnoreCase(categoria.getNome())) {
            throw new RecursoDuplicadoException("Categoria ja cadastrada");
        }
        return repository.save(categoria);
    }

    @Transactional(readOnly = true)
    public CategoriaInstrumento buscarPorId(Long id){
        return repository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Categoria não encontrada")
                );
    }

    @Transactional(readOnly = true)
    public List<CategoriaInstrumento> listar() {
        return repository.findAll();
    }
}
