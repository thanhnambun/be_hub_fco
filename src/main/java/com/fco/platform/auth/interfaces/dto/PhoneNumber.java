package com.fco.platform.auth.interfaces.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface PhoneNumber {
    String message() default "Số điện thoại không hợp lệ (Phải bắt đầu bằng 03, 05, 07, 08, 09 và gồm 10 số)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
