package com.curso.instrumento2026.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class FornecedorTest {
    @Test
    void deveCriarFornecedorComCnpjValido() {
        var fornecedor = new Fornecedor("Loja de instrumentos", "11222333000181");
        assertEquals("11222333000181", fornecedor.getCnpj());
        assertEquals("Loja de instrumentos", fornecedor.getRazaoSocial());
        assertEquals(Status.ATIVO, fornecedor.getStatus());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"11222333000182", "11111111111111", "00000000000000",
            "11.222.333/0001-81", "1122233300018", "11222333000181 ", "1122233300018A"})
    void deveRejeitarCnpjInvalidoMesmoForaDaApi(String cnpj) {
        var erro = assertThrows(IllegalArgumentException.class, () -> new Fornecedor("Fornecedor", cnpj));
        assertEquals("CNPJ inválido", erro.getMessage());
    }
}
