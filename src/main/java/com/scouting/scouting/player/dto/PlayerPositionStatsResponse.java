package com.scouting.scouting.player.dto;

import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PositionGroup;

public record PlayerPositionStatsResponse(
        PlayerPosition position,
        PositionGroup positionGroup,
        PlayerStatsTotalsResponse totals
) {
}
