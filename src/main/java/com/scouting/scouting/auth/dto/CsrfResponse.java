package com.scouting.scouting.auth.dto;

/**
 * Token CSRF para operaciones de escritura. El cliente lo envia en el header
 * indicado en {@code headerName}; el token se guarda en la sesion del servidor.
 */
public record CsrfResponse(String token, String headerName, String parameterName) {
}
