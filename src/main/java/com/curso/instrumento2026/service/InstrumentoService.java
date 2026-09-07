package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.domain.Fornecedor;
import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.CategoriaInstrumentoRepository;
import com.curso.instrumento2026.repository.FornecedorRepository;
import com.curso.instrumento2026.repository.InstrumentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstrumentoService {

    private final InstrumentoRepository instrumentoRepository;
    private final CategoriaInstrumentoRepository categoriaRepository;
    private final FornecedorRepository fornecedorRepository;

    public InstrumentoService(
            InstrumentoRepository instrumentoRepository,
            CategoriaInstrumentoRepository categoriaRepository,
            FornecedorRepository fornecedorRepository) {

        this.instrumentoRepository = instrumentoRepository;
        this.categoriaRepository = categoriaRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    @Transactional
    public Instrumento cadastrar(
            Instrumento instrumento,
            Long categoriaId,
            Long fornecedorId) {

        if (instrumentoRepository.existsByCodigoInstrumento(
                instrumento.getCodigoInstrumento())) {

            throw new RecursoDuplicadoException(
                    "Código do instrumento já cadastrado"
            );
        }

        CategoriaInstrumento categoria =
                categoriaRepository.findById(categoriaId)
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "Categoria não encontrada"
                                )
                        );

        Fornecedor fornecedor = null;

        if (fornecedorId != null) {
            fornecedor = fornecedorRepository.findById(fornecedorId)
                    .orElseThrow(() ->
                            new RecursoNaoEncontradoException(
                                    "Fornecedor não encontrado"
                            )
                    );
        }

        instrumento.definirCategoria(categoria);
        instrumento.definirFornecedor(fornecedor);

        categoria.adicionarInstrumento(instrumento);

        return instrumentoRepository.save(instrumento);
    }

    @Transactional(readOnly = true)
    public Instrumento buscarPorId(Long id) {

        return instrumentoRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Instrumento não encontrado"
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<Instrumento> listar() {
        return instrumentoRepository.findAll();
    }
}

