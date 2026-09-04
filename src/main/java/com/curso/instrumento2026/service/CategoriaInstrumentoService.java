package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.CategoriaInstrumentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaInstrumentoService {
    private final CategoriaInstrumentoRepository repository;

    public CategoriaInstrumentoService(CategoriaInstrumentoRepository repository) {
        this.repository = repository;
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
