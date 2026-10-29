package com.scouting.scouting.player.dto;

public record ResultStatsResponse(
        StatCountResponse wins,
        StatCountResponse draws,
        StatCountResponse losses,
        int unknownResults,
        StatAverageResponse winRate
) {
}
