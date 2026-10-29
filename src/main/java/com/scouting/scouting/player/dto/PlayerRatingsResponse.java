package com.scouting.scouting.player.dto;

public record PlayerRatingsResponse(
        StatAverageResponse technical,
        StatAverageResponse tactical,
        StatAverageResponse physical,
        StatAverageResponse mental,
        StatAverageResponse overall
) {
}
