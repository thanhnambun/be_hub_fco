package com.ra.base_spring_boot.validate.impl;

import com.ra.base_spring_boot.validate.ValidPhone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPhoneImpl implements ConstraintValidator<ValidPhone, String> {

    private static final String PHONE_REGEX = "^(0[3|5|7|8|9][0-9]{8}|(\\+84)[3|5|7|8|9][0-9]{8})$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        return value.matches(PHONE_REGEX);
    }
}