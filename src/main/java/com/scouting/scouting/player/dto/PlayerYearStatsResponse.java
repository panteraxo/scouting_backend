package com.scouting.scouting.player.dto;

public record PlayerYearStatsResponse(
        int year,
        PlayerStatsTotalsResponse totals
) {
}
