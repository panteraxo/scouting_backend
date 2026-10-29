package com.scouting.scouting.recommendation.dto;

import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PositionGroup;

/** Resumen deportivo minimo del jugador para listados. */
public record RecommendationPlayerSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        Integer age,
        PlayerPosition position,
        PositionGroup positionGroup
) {
}
