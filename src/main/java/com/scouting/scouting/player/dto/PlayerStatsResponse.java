package com.scouting.scouting.player.dto;

import java.util.List;

public record PlayerStatsResponse(
        Long playerId,
        PlayerStatsTotalsResponse career,
        List<PlayerYearStatsResponse> byYear,
        List<PlayerActivityTypeStatsResponse> byActivityType,
        List<PlayerPositionStatsResponse> byPosition,
        List<PlayerPositionGroupStatsResponse> byPositionGroup,
        PlayerRatingsResponse ratings
) {
}
