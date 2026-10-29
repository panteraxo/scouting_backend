package com.scouting.scouting.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restringe un texto a una URI absoluta http o https con host presente.
 * No descarga ni resuelve la URL; solo valida su forma.
 */
@Documented
@Constraint(validatedBy = HttpUrlValidator.class)
@Target({
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.RECORD_COMPONENT,
        ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpUrl {

    String message() default "Debe ser una URL http o https valida.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
