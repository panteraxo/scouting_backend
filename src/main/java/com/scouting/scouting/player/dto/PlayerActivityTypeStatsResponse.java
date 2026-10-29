package com.scouting.scouting.player.dto;

import com.scouting.scouting.player.entity.ActivityType;

public record PlayerActivityTypeStatsResponse(
        ActivityType activityType,
        boolean matchLike,
        int entries,
        PlayerStatsTotalsResponse totals
) {
}
