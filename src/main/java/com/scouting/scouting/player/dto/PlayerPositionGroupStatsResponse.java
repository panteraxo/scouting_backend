package com.scouting.scouting.player.dto;

import com.scouting.scouting.player.entity.PositionGroup;

public record PlayerPositionGroupStatsResponse(
        PositionGroup positionGroup,
        PlayerStatsTotalsResponse totals
) {
}
