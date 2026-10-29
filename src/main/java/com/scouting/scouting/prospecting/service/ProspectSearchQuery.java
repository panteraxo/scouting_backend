package com.scouting.scouting.prospecting.service;

/** Escapado literal del filtro de texto de prospecciones. */
public final class ProspectSearchQuery {

    private static final char ESCAPE = '!';

    private ProspectSearchQuery() {
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
