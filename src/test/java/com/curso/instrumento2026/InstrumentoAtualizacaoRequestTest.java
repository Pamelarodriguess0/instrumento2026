package com.curso.instrumento2026;

import com.curso.instrumento2026.api.dto.InstrumentoAtualizacaoRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class InstrumentoAtualizacaoRequestTest {
    @Test
    void deveExigirPrecoPositivoMantendoFornecedorOpcional() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (BigDecimal preco : new BigDecimal[]{null, BigDecimal.ZERO, BigDecimal.ONE.negate()}) {
                var request = new InstrumentoAtualizacaoRequest("Violino", preco, BigDecimal.ZERO, 1L, null);
                assertTrue(validator.validate(request).stream().anyMatch(
                        erro -> erro.getPropertyPath().toString().equals("precoUnitario")));
            }
            assertTrue(validator.validate(new InstrumentoAtualizacaoRequest(
                    "Violino", new BigDecimal("0.01"), BigDecimal.ZERO, 1L, null)).isEmpty());
        }
    }
}
