package com.scouting.scouting.recommendation.dto;

import com.scouting.scouting.recommendation.entity.RecommendationResponseStatus;
import com.scouting.scouting.recommendation.entity.RecommendationStatus;

import java.time.Instant;

/** Metadatos de auditoria para ADMIN; sin carta ni snapshot. */
public record RecommendationAdminResponse(
        Long id,
        Long prospectId,
        Long playerId,
        String playerFirstName,
        String playerLastName,
        Long scoutId,
        String scoutFirstName,
        String scoutLastName,
        Long clubId,
        String clubCode,
        String clubName,
        RecommendationStatus status,
        RecommendationResponseStatus lastResponseStatus,
        Instant lastResponseAt,
        Instant sentAt,
        Instant createdAt
) {
}
