package com.ra.base_spring_boot.validate.impl;

import com.ra.base_spring_boot.validate.ValidEmail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidEmailImpl implements ConstraintValidator<ValidEmail, String> {
    private static final String GMAIL_REGEX = "^[A-Za-z][A-Za-z0-9]{4,}@gmail\\.com$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        return value.matches(GMAIL_REGEX);
    }
}
