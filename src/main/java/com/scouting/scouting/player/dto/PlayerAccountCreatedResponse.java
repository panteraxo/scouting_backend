package com.scouting.scouting.player.dto;

import java.time.Instant;

/**
 * Resultado del alta conjunta de cuenta y perfil.
 *
 * <p>{@code player} contiene la proyeccion de perfil correspondiente al rol del
 * actor (ADMIN o staff). La contrasena temporal solo se devuelve en esta
 * respuesta, una unica vez, y el endpoint responde con {@code no-store}.</p>
 */
public record PlayerAccountCreatedResponse(
        Object player,
        String temporaryPassword,
        Instant temporaryPasswordExpiresAt
) {
}
