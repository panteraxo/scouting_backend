package com.scouting.scouting.recommendation.dto;

import com.scouting.scouting.recommendation.entity.RecommendationStatus;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public record RecommendationDetailResponse(
        Long id,
        Long prospectId,
        RecommendationPlayerSummaryResponse player,
        RecommendationScoutRefResponse scout,
        RecommendationClubRefResponse club,
        RecommendationStatus status,
        String message,
        JsonNode snapshot,
        List<RecommendationResponseEntry> responses,
        Instant sentAt,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
