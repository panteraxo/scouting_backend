package com.scouting.scouting.player.dto;

import com.scouting.scouting.player.entity.GuardianConsentStatus;

import java.time.Instant;

public record GuardianConsentResponse(
        GuardianConsentStatus status,
        Instant recordedAt,
        ActorSummaryResponse recordedBy,
        String evidenceUrl,
        String note
) {
}
