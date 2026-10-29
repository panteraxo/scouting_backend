package com.scouting.scouting.auth.dto;

/**
 * Respuesta generica del registro publico. Se devuelve el mismo mensaje exista o
 * no un duplicado para no revelar si un correo, documento o usuario ya esta en uso.
 */
public record ScoutRegistrationResponse(String message) {
}
