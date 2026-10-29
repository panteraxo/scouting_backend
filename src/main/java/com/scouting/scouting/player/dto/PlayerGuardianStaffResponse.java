package com.scouting.scouting.player.dto;

import com.scouting.scouting.user.entity.DocumentType;

import java.time.Instant;

public record PlayerGuardianStaffResponse(
        Long playerId,
        boolean minor,
        String firstName,
        String lastName,
        DocumentType documentType,
        String documentNumber,
        String phone,
        String address,
        String relationship,
        GuardianConsentResponse consent,
        Long version,
        Instant updatedAt
) {
}
