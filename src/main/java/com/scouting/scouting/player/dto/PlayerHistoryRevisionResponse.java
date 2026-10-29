package com.scouting.scouting.player.dto;

import com.scouting.scouting.player.entity.HistoryChangeType;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record PlayerHistoryRevisionResponse(
        Integer revisionNumber,
        HistoryChangeType changeType,
        String reason,
        ActorSummaryResponse editedBy,
        Instant editedAt,
        JsonNode snapshot
) {
}
