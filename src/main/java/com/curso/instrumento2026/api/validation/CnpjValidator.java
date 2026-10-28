package com.curso.instrumento2026.api.validation;

import com.curso.instrumento2026.domain.DocumentoFiscal;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidator implements ConstraintValidator<CnpjValido, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || DocumentoFiscal.cnpjValido(value);
    }
}
