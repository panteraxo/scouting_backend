package com.scouting.scouting.recommendation.dto;

import com.scouting.scouting.recommendation.entity.RecommendationResponseStatus;
import com.scouting.scouting.recommendation.entity.RecommendationStatus;

import java.time.Instant;

public record RecommendationSummaryResponse(
        Long id,
        Long prospectId,
        RecommendationPlayerSummaryResponse player,
        RecommendationScoutRefResponse scout,
        RecommendationClubRefResponse club,
        RecommendationStatus status,
        Instant sentAt,
        RecommendationResponseStatus lastResponseStatus,
        Instant lastResponseAt
) {
}
