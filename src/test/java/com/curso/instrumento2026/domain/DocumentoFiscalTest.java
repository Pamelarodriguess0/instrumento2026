package com.curso.instrumento2026.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentoFiscalTest {
    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "11144477735", "01234567890", "12345678909"})
    void deveAceitarCpfComDigitosVerificadoresCorretos(String cpf) {
        assertTrue(DocumentoFiscal.cpfValido(cpf));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "52998224724", "52998224735", "5299822472", "529982247250",
            "529.982.247-25", " 52998224725", "52998224725 ", "52998 24725",
            "5299822472A", "５２９９８２２４７２５", "           "
    })
    void deveRejeitarCpfInvalidoSemNormalizarEntrada(String cpf) {
        assertFalse(DocumentoFiscal.cpfValido(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00000000000", "11111111111", "22222222222", "33333333333", "44444444444",
            "55555555555", "66666666666", "77777777777", "88888888888", "99999999999"
    })
    void deveRejeitarCpfComTodosOsDigitosRepetidos(String cpf) {
        assertFalse(DocumentoFiscal.cpfValido(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000181", "04252011000110", "11444777000161"})
    void deveAceitarCnpjComDigitosVerificadoresCorretos(String cnpj) {
        assertTrue(DocumentoFiscal.cnpjValido(cnpj));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "11222333000182", "11222333000191", "1122233300018", "112223330001810",
            "11.222.333/0001-81", " 11222333000181", "11222333000181 ", "11222333 00181",
            "1122233300018A", "１１２２２３３３０００１８１", "              "
    })
    void deveRejeitarCnpjInvalidoSemNormalizarEntrada(String cnpj) {
        assertFalse(DocumentoFiscal.cnpjValido(cnpj));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00000000000000", "11111111111111", "22222222222222", "33333333333333", "44444444444444",
            "55555555555555", "66666666666666", "77777777777777", "88888888888888", "99999999999999"
    })
    void deveRejeitarCnpjComTodosOsDigitosRepetidos(String cnpj) {
        assertFalse(DocumentoFiscal.cnpjValido(cnpj));
    }
}
