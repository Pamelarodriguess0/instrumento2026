package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.CategoriaInstrumentoRepository;
import com.curso.instrumento2026.repository.InstrumentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstrumentoService {
    private final InstrumentoRepository instrumentoRepository;
    private final CategoriaInstrumentoRepository categoriaRepository;

    public InstrumentoService ( InstrumentoRepository instrumentoRepository,
    CategoriaInstrumentoRepository catetegoriaRepository) {

        this.instrumentoRepository = instrumentoRepository;
        this.categoriaRepository = catetegoriaRepository;
    }
    @Transactional
    public Instrumento cadastrar(Instrumento instrumento) {

        if(instrumentoRepository.existsByCodigoInstrumento(instrumento.getCodigoInstrumento())) {
            throw new RecursoDuplicadoException(
                  "Codigo do instrumento já cadastrado"
            );
        }

        Long categoriaId = instrumento.getCategoria().getId();
        CategoriaInstrumento categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Categoria não encontrada")
                );
        categoria.adicionarInstrumento(instrumento);
        return instrumentoRepository.save(instrumento);
    }

    @Transactional(readOnly = true)
    public Instrumento buscarPorId(Long id){
        return  instrumentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Instrumento não Encontrado")
                );
    }

    @Transactional(readOnly = true)
    public List<Instrumento> listar() {
        return instrumentoRepository.findAll();
    }
}
