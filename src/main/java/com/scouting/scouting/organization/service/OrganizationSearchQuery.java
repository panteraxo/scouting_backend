package com.scouting.scouting.organization.service;

/**
 * Normaliza el filtro de texto {@code q} para las busquedas de organizaciones.
 *
 * <p>El valor se interpreta de forma literal: {@code %}, {@code _} y el propio
 * caracter de escape {@code !} se escapan antes de construir el patron LIKE, de
 * modo que el cliente no puede ampliar el resultado con comodines. Los
 * repositorios declaran {@code escape '!'} para que las base de datos traten
 * ese caracter como escape.</p>
 */
public final class OrganizationSearchQuery {

    /** Caracter de escape usado en el patron LIKE y declarado en los repositorios. */
    public static final char ESCAPE = '!';

    private OrganizationSearchQuery() {
    }

    /**
     * Devuelve un patron {@code contains} escapado, o {@code null} si el valor
     * es nulo o en blanco. El valor se recorta antes de escapar.
     */
    public static String containsPattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        StringBuilder pattern = new StringBuilder(trimmed.length() + 2);
        pattern.append('%');
        for (int i = 0; i < trimmed.length(); i++) {
            char current = trimmed.charAt(i);
            if (current == ESCAPE || current == '%' || current == '_') {
                pattern.append(ESCAPE);
            }
            pattern.append(current);
        }
        return pattern.append('%').toString();
    }
}
