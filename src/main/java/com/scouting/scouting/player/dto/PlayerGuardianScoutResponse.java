package com.scouting.scouting.player.dto;

import com.scouting.scouting.user.entity.DocumentType;

public record PlayerGuardianScoutResponse(
        Long playerId,
        boolean minor,
        String firstName,
        String lastName,
        DocumentType documentType,
        String documentNumber,
        String phone,
        String address,
        String relationship,
        GuardianConsentSummaryResponse consent
) {
}
