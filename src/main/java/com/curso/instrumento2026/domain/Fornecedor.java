package com.curso.instrumento2026.domain;

import jakarta.persistence.*;

@Entity
@Table(
        name = "fornecedor",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fornecedor_cnpj",
                columnNames = "cnpj"
        )
)
public class Fornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razao_social", nullable = false, length = 150)
    private String razaoSocial;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    protected Fornecedor() {
    }

    public Fornecedor(String razaoSocial, String cnpj) {
        this.razaoSocial = validarTextoObrigatorio(razaoSocial);
        this.cnpj = validarCnpj(cnpj);
        this.status = Status.ATIVO;
    }

    private String validarTextoObrigatorio(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Razão social é obrigatória");
        }

        return texto;
    }

    private String validarCnpj(String cnpj) {
        if (cnpj == null || !cnpj.matches("\\d{14}")) {
            throw new IllegalArgumentException(
                    "CNPJ deve conter 14 dígitos"
            );
        }

        return cnpj;
    }

    public Long getId() {
        return id;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getCnpj() {
        return cnpj;
    }

    public Status getStatus() {
        return status;
    }
}