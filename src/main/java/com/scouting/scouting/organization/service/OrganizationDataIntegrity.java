package com.scouting.scouting.organization.service;

import com.scouting.scouting.exception.ApiException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;

/**
 * Traduce de forma conservadora las violaciones de unicidad de RUC.
 *
 * <p>Solo se transforma el error cuando la restriccion implicada coincide
 * exactamente con el indice unico de RUC de la entidad. Cualquier otra
 * violacion de integridad se propaga sin cambios para no ocultar errores
 * distintos (por ejemplo, colisiones de codigo visible o fallos de datos).</p>
 */
public final class OrganizationDataIntegrity {

    public static final String ACADEMY_RUC_CONSTRAINT = "uk_academies_ruc";
    public static final String CLUB_RUC_CONSTRAINT = "uk_clubs_ruc";

    private OrganizationDataIntegrity() {
    }

    /**
     * Indica si la excepcion corresponde a la restriccion unica de RUC
     * indicada. Se revisa el nombre de restriccion de Hibernate y la cadena de
     * mensajes de las causas, porque el nombre expuesto varia entre MySQL y
     * H2. Si el proveedor no expone la restriccion, devuelve {@code false}.
     */
    public static boolean isRucConflict(DataIntegrityViolationException ex, String constraintName) {
        if (ex == null || constraintName == null || constraintName.isBlank()) {
            return false;
        }
        String expected = constraintName.toLowerCase(Locale.ROOT);
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConstraintViolationException hibernateViolation
                    && matches(hibernateViolation.getConstraintName(), expected)) {
                return true;
            }
            if (matches(current.getMessage(), expected)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Devuelve la excepcion funcional de RUC duplicado o {@code null} si el
     * error no puede atribuirse con seguridad a esa restriccion.
     */
    public static ApiException rucConflictOrNull(DataIntegrityViolationException ex,
                                                 String constraintName,
                                                 String message) {
        return isRucConflict(ex, constraintName)
                ? ApiException.conflict("RUC_ALREADY_EXISTS", message)
                : null;
    }

    private static boolean matches(String value, String expected) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(expected);
    }
}
