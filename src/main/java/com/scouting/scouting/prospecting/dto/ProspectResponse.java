package com.scouting.scouting.prospecting.dto;

import com.scouting.scouting.prospecting.entity.ProspectStatus;

import java.time.Instant;

public record ProspectResponse(
        Long id,
        ProspectPlayerSummaryResponse player,
        ProspectStatus status,
        String privateNotes,
        Instant startedAt,
        Instant lastActivityAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
