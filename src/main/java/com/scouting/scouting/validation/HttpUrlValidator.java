package com.scouting.scouting.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Valida la forma de una URL sin realizar peticiones de red.
 *
 * <p>Se acepta unicamente una URI absoluta, con esquema http o https y host
 * no vacio. Los valores nulos o en blanco se dejan a las restricciones de
 * obligatoriedad ({@code @NotBlank}) o a la normalizacion de campos opcionales.</p>
 */
public class HttpUrlValidator implements ConstraintValidator<HttpUrl, String> {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        final URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException ex) {
            return false;
        }

        if (!uri.isAbsolute() || uri.isOpaque()) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            return false;
        }
        String host = uri.getHost();
        return host != null && !host.isBlank();
    }
}
