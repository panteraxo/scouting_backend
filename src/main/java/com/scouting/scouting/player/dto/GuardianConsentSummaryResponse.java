package com.scouting.scouting.player.dto;

import com.scouting.scouting.player.entity.GuardianConsentStatus;

import java.time.Instant;

public record GuardianConsentSummaryResponse(
        GuardianConsentStatus status,
        Instant recordedAt
) {
}
