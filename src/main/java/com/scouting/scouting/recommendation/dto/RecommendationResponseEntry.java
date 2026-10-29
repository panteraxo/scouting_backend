package com.scouting.scouting.recommendation.dto;

import com.scouting.scouting.recommendation.entity.RecommendationResponseStatus;

import java.time.Instant;

public record RecommendationResponseEntry(
        Long id,
        RecommendationResponseStatus status,
        String message,
        Instant respondedAt
) {
}
