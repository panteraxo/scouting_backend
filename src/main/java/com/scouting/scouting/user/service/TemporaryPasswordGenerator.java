package com.scouting.scouting.user.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Genera contrasenas temporales de un solo uso. No se registra ni persiste el
 * valor en claro; solo se devuelve una vez en la respuesta de creacion/reseteo.
 */
@Component
public class TemporaryPasswordGenerator {

    static final Duration TEMPORARY_PASSWORD_TTL = Duration.ofHours(24);
    private static final int PASSWORD_LENGTH = 16;
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%&*";
    private static final String ALL = LOWER + UPPER + DIGITS + SYMBOLS;

    private final SecureRandom secureRandom = new SecureRandom();

    public GeneratedTemporaryPassword generate() {
        List<Character> characters = new ArrayList<>(PASSWORD_LENGTH);
        characters.add(randomFrom(LOWER));
        characters.add(randomFrom(UPPER));
        characters.add(randomFrom(DIGITS));
        characters.add(randomFrom(SYMBOLS));
        while (characters.size() < PASSWORD_LENGTH) {
            characters.add(randomFrom(ALL));
        }
        Collections.shuffle(characters, secureRandom);

        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (char character : characters) {
            password.append(character);
        }
        return new GeneratedTemporaryPassword(password.toString(), Instant.now().plus(TEMPORARY_PASSWORD_TTL));
    }

    private char randomFrom(String source) {
        return source.charAt(secureRandom.nextInt(source.length()));
    }

    public record GeneratedTemporaryPassword(String password, Instant expiresAt) {
    }
}
