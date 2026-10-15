package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.domain.Fornecedor;
import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.domain.Status;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.CategoriaInstrumentoRepository;
import com.curso.instrumento2026.repository.FornecedorRepository;
import com.curso.instrumento2026.repository.InstrumentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@Service
public class InstrumentoService {
    @Transactional(readOnly = true)
    public Page<Instrumento> pesquisar(String nomeInstrumento, Status status,
                                     Long categoriaId, Long fornecedorId,
                                     Boolean abaixoEstoqueMinimo, Pageable pageable) {
        validarOrdenacao(pageable, Set.of("id", "codigoInstrumento", "nomeInstrumento",
                "quantidadeEstoque", "precoUnitario", "estoqueMinimo", "dataCadastro", "status"));
        Specification<Instrumento> filtros = (root, query, cb) -> cb.conjunction();
        if (nomeInstrumento != null && !nomeInstrumento.isBlank()) {
            String trecho = "%" + nomeInstrumento.trim().toLowerCase(Locale.ROOT) + "%";
            filtros = filtros.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("nomeInstrumento")), trecho));
        }
        if (status != null) {
            filtros = filtros.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (categoriaId != null) {
            filtros = filtros.and((root, query, cb) -> cb.equal(root.get("categoria").get("id"), categoriaId));
        }
        if (fornecedorId != null) {
            filtros = filtros.and((root, query, cb) -> cb.equal(root.get("fornecedor").get("id"), fornecedorId));
        }
        if (Boolean.TRUE.equals(abaixoEstoqueMinimo)) {
            filtros = filtros.and((root, query, cb) -> cb.lessThan(
                    root.get("quantidadeEstoque").as(BigDecimal.class), root.get("estoqueMinimo")));
        }
        return instrumentoRepository.findAll(filtros, pageable);
    }

    private void validarOrdenacao(Pageable pageable, Set<String> camposPermitidos) {
        pageable.getSort().forEach(ordem -> {
            if (!camposPermitidos.contains(ordem.getProperty())) {
                throw new IllegalArgumentException("Campo de ordenação inválido: " + ordem.getProperty());
            }
        });
    }


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

    @Transactional
    public Instrumento alterar(Long id, String nomeInstrumento,
                              BigDecimal precoUnitario, BigDecimal estoqueMinimo,
                              Long categoriaId, Long fornecedorId) {
        Instrumento instrumento = buscarPorId(id);
        CategoriaInstrumento categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
        Fornecedor fornecedor = fornecedorId == null ? null
                : fornecedorRepository.findById(fornecedorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado"));
        instrumento.alterarNome(nomeInstrumento);
        instrumento.alterarPrecoUnitario(precoUnitario);
        instrumento.alterarEstoqueMinimo(estoqueMinimo);
        instrumento.alterarCategoria(categoria);
        instrumento.alterarFornecedor(fornecedor);
        return instrumento;
    }

    @Transactional
    public Instrumento alterarStatus(Long id, Status status) {
        Instrumento instrumento = buscarPorId(id);
        if (status == Status.ATIVO) {
            instrumento.ativar();
        } else {
            instrumento.inativar();
        }
        return instrumento;
    }

    @Transactional
    public void excluir(Long id) {
        Instrumento instrumento = buscarPorId(id);
        instrumentoRepository.delete(instrumento);
        instrumentoRepository.flush();
    }

    @Transactional
    public Instrumento receberEstoque(Long id, BigDecimal quantidade) {
        Instrumento instrumento = buscarPorId(id);
        instrumento.adicionarEstoque(quantidadeInteira(quantidade));
        return instrumento;
    }

    @Transactional
    public Instrumento retirarEstoque(Long id, BigDecimal quantidade) {
        Instrumento instrumento = buscarPorId(id);
        instrumento.retirarEstoque(quantidadeInteira(quantidade));
        return instrumento;
    }

    private int quantidadeInteira(BigDecimal quantidade) {
        if (quantidade == null) {
            throw new IllegalArgumentException("Quantidade é obrigatória");
        }
        try {
            return quantidade.intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Quantidade deve ser inteira e estar no intervalo permitido", exception);
        }
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

