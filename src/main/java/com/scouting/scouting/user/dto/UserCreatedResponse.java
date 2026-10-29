package com.scouting.scouting.user.dto;

import java.time.Instant;

/**
 * Respuesta de creacion o de reseteo de contrasena. La contrasena temporal solo
 * se devuelve en este response (una unica vez) y nunca se guarda en claro ni se
 * registra en logs. El endpoint debe enviarse con {@code Cache-Control: no-store}.
 */
public record UserCreatedResponse(
        UserSummaryResponse user,
        String temporaryPassword,
        Instant temporaryPasswordExpiresAt
) {
}
