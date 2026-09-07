package com.curso.instrumento2026.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "instrumento")
public class Instrumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "codigo_instrumento",
            nullable = false,
            unique = true,
            length = 50
    )
    private String codigoInstrumento;

    @Column(
            name = "nome_instrumento",
            nullable = false,
            length = 150
    )
    private String nomeInstrumento;

    @Column(
            name = "quantidade_estoque",
            nullable = false
    )
    private int quantidadeEstoque;

    @Column(
            name = "estoque_minimo",
            nullable = false,
            precision = 18,
            scale = 3
    )
    private BigDecimal estoqueMinimo;

    @Column(
            name = "preco_unitario",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal precoUnitario;

    @Column(
            name = "data_cadastro",
            nullable = false
    )
    private LocalDate dataCadastro;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private Status status;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "categoria_instrumento_id",
            nullable = false
    )
    private CategoriaInstrumento categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "fornecedor_id",
            foreignKey = @ForeignKey(
                    name = "fk_instrumento_fornecedor"
            )
    )
    private Fornecedor fornecedor;

    protected Instrumento() {
    }

    public Instrumento(
            Long id,
            String codigoInstrumento,
            String nomeInstrumento,
            int quantidadeEstoque,
            BigDecimal precoUnitario,
            LocalDate dataCadastro,
            Status status,
            CategoriaInstrumento categoria) {

        this(
                id,
                codigoInstrumento,
                nomeInstrumento,
                quantidadeEstoque,
                precoUnitario,
                BigDecimal.ZERO,
                dataCadastro,
                status,
                categoria,
                null
        );
    }

    public Instrumento(
            Long id,
            String codigoInstrumento,
            String nomeInstrumento,
            int quantidadeEstoque,
            BigDecimal precoUnitario,
            BigDecimal estoqueMinimo,
            LocalDate dataCadastro,
            Status status,
            CategoriaInstrumento categoria,
            Fornecedor fornecedor) {

        validarCodigo(codigoInstrumento);
        validarNome(nomeInstrumento);
        validarQuantidadeEstoque(quantidadeEstoque);
        validarPrecoUnitario(precoUnitario);
        validarEstoqueMinimo(estoqueMinimo);

        if (dataCadastro == null) {
            throw new IllegalArgumentException(
                    "Data de cadastro é obrigatória"
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "Status é obrigatório"
            );
        }

        if (categoria == null) {
            throw new IllegalArgumentException(
                    "Categoria é obrigatória"
            );
        }

        this.id = id;
        this.codigoInstrumento = codigoInstrumento;
        this.nomeInstrumento = nomeInstrumento;
        this.quantidadeEstoque = quantidadeEstoque;
        this.precoUnitario = precoUnitario;
        this.estoqueMinimo = estoqueMinimo;
        this.dataCadastro = dataCadastro;
        this.status = status;
        this.categoria = categoria;
        this.fornecedor = fornecedor;
    }

    public Instrumento(
            String codigoInstrumento,
            String nomeInstrumento,
            int quantidadeEstoque,
            BigDecimal precoUnitario,
            BigDecimal estoqueMinimo,
            LocalDate dataCadastro) {

        validarCodigo(codigoInstrumento);
        validarNome(nomeInstrumento);
        validarQuantidadeEstoque(quantidadeEstoque);
        validarPrecoUnitario(precoUnitario);
        validarEstoqueMinimo(estoqueMinimo);

        if (dataCadastro == null) {
            throw new IllegalArgumentException(
                    "Data de cadastro é obrigatória"
            );
        }

        this.codigoInstrumento = codigoInstrumento;
        this.nomeInstrumento = nomeInstrumento;
        this.quantidadeEstoque = quantidadeEstoque;
        this.precoUnitario = precoUnitario;
        this.estoqueMinimo = estoqueMinimo;
        this.dataCadastro = dataCadastro;
        this.status = Status.ATIVO;
    }

    private void validarCodigo(String codigoInstrumento) {
        if (codigoInstrumento == null || codigoInstrumento.isBlank()) {
            throw new IllegalArgumentException(
                    "Código do instrumento é obrigatório"
            );
        }
    }

    private void validarNome(String nomeInstrumento) {
        if (nomeInstrumento == null || nomeInstrumento.isBlank()) {
            throw new IllegalArgumentException(
                    "Nome do instrumento é obrigatório"
            );
        }
    }

    private void validarQuantidadeEstoque(int quantidadeEstoque) {
        if (quantidadeEstoque < 0) {
            throw new IllegalArgumentException(
                    "Quantidade em estoque não pode ser negativa"
            );
        }
    }

    private void validarPrecoUnitario(BigDecimal precoUnitario) {
        if (precoUnitario == null ||
                precoUnitario.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Preço unitário deve ser maior que zero"
            );
        }
    }

    private void validarEstoqueMinimo(BigDecimal estoqueMinimo) {
        if (estoqueMinimo == null ||
                estoqueMinimo.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Estoque mínimo não pode ser negativo"
            );
        }
    }

    public BigDecimal calcularValorEstoque() {
        return precoUnitario.multiply(
                BigDecimal.valueOf(quantidadeEstoque)
        );
    }

    public void adicionarEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException(
                    "Quantidade adicionada deve ser maior que zero"
            );
        }

        this.quantidadeEstoque += quantidade;
    }

    public void retirarEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException(
                    "Quantidade deve ser maior que zero"
            );
        }

        if (quantidade > quantidadeEstoque) {
            throw new IllegalArgumentException(
                    "Estoque insuficiente"
            );
        }

        this.quantidadeEstoque -= quantidade;
    }

    public void alterarNome(String novoNome) {
        validarNome(novoNome);

        this.nomeInstrumento = novoNome;
    }

    public void alterarPrecoUnitario(BigDecimal novoPreco) {
        validarPrecoUnitario(novoPreco);

        this.precoUnitario = novoPreco;
    }

    public void ativar() {
        this.status = Status.ATIVO;
    }

    public void inativar() {
        this.status = Status.INATIVO;
    }

    public void definirCategoria(
            CategoriaInstrumento categoria) {

        if (categoria == null) {
            throw new IllegalArgumentException(
                    "Categoria é obrigatória"
            );
        }

        this.categoria = categoria;
    }

    public void definirFornecedor(
            Fornecedor fornecedor) {

        this.fornecedor = fornecedor;
    }

    public Long getId() {
        return id;
    }

    public String getCodigoInstrumento() {
        return codigoInstrumento;
    }

    public String getNomeInstrumento() {
        return nomeInstrumento;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public BigDecimal getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public LocalDate getDataCadastro() {
        return dataCadastro;
    }

    public Status getStatus() {
        return status;
    }

    public CategoriaInstrumento getCategoria() {
        return categoria;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }
}