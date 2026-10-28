package com.curso.instrumento2026.api.validation;

import com.curso.instrumento2026.api.dto.FornecedorRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoFiscalValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    record CpfOpcional(@CpfValido String cpf) { }
    record CnpjOpcional(@CnpjValido String cnpj) { }
    record CpfObrigatorio(@NotBlank(message = "CPF é obrigatório") @CpfValido String cpf) { }

    @BeforeAll
    static void iniciar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void encerrar() {
        factory.close();
    }

    @Test
    void validadoresEspecificosDevemAceitarNulo() {
        assertTrue(validator.validate(new CpfOpcional(null)).isEmpty());
        assertTrue(validator.validate(new CnpjOpcional(null)).isEmpty());
    }

    @Test
    void obrigatoriedadeDeveSerResponsabilidadeDoNotBlank() {
        var cpf = validator.validate(new CpfObrigatorio(null));
        assertEquals(1, cpf.size());
        assertEquals("CPF é obrigatório", cpf.iterator().next().getMessage());
        var cnpj = validator.validate(new FornecedorRequest("Fornecedor", null));
        assertEquals(1, cnpj.size());
        assertEquals("CNPJ é obrigatório", cnpj.iterator().next().getMessage());
    }

    @Test
    void deveAceitarDocumentosValidosNosRecords() {
        assertTrue(validator.validate(new CpfObrigatorio("52998224725")).isEmpty());
        assertTrue(validator.validate(new FornecedorRequest("Fornecedor", "11222333000181")).isEmpty());
    }

    @Test
    void deveRejeitarCpfComVerificadoresInvalidosRepetidosOuFormatacao() {
        for (String cpf : new String[]{"52998224724", "11111111111", "529.982.247-25", "", "  "}) {
            var erros = validator.validate(new CpfOpcional(cpf));
            assertEquals(1, erros.size());
            assertEquals("cpf", erros.iterator().next().getPropertyPath().toString());
            assertEquals("CPF inválido", erros.iterator().next().getMessage());
        }
    }

    @Test
    void deveRejeitarCnpjComVerificadoresInvalidosRepetidosOuFormatacao() {
        for (String cnpj : new String[]{"11222333000182", "11111111111111", "11.222.333/0001-81", "", "  "}) {
            var erros = validator.validate(new CnpjOpcional(cnpj));
            assertEquals(1, erros.size());
            assertEquals("cnpj", erros.iterator().next().getPropertyPath().toString());
            assertEquals("CNPJ inválido", erros.iterator().next().getMessage());
        }
    }

    @Test
    void fornecedorRequestDeveAplicarAValidacaoFiscal() {
        var erros = validator.validate(new FornecedorRequest("Fornecedor", "11222333000182"));
        assertEquals(1, erros.size());
        assertEquals("CNPJ inválido", erros.iterator().next().getMessage());
        assertEquals("cnpj", erros.iterator().next().getPropertyPath().toString());
    }
}
