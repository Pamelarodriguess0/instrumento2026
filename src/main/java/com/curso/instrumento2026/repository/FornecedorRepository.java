package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.domain.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorRepository
        extends JpaRepository<Fornecedor, Long> {

    boolean existsByCnpj(String cnpj);
}