package com.scouting.scouting.player.dto;

import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.VerificationStatus;

/** Referencia publica de la academia del jugador, derivada de UserOrganization. */
public record AcademyRefResponse(
        Long id,
        String code,
        String name,
        String logoUrl,
        OperationalStatus operationalStatus,
        VerificationStatus verificationStatus
) {
}
