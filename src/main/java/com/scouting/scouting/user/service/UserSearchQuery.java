package com.scouting.scouting.user.service;

/**
 * Normaliza el filtro de texto {@code q} de usuarios.
 *
 * <p>El valor se interpreta de forma literal: {@code %}, {@code _} y el propio
 * caracter de escape {@code !} se escapan antes de construir el patron LIKE, de
 * modo que el cliente no puede ampliar el resultado con comodines. El
 * repositorio declara {@code escape '!'}.</p>
 */
public final class UserSearchQuery {

    public static final char ESCAPE = '!';

    private UserSearchQuery() {
    }

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
