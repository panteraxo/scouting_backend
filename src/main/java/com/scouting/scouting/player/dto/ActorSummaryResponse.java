package com.scouting.scouting.player.dto;

import com.scouting.scouting.user.entity.Role;

/** Identificacion minima de un actor; nunca incluye correo, documento ni telefono. */
public record ActorSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        Role role
) {
}
