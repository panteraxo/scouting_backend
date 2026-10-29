package com.scouting.scouting.player.dto;

public record PlayerStatsTotalsResponse(
        StatCountResponse matchesPlayed,
        Integer matchesRecorded,
        StatCountResponse minutes,
        StatCountResponse goals,
        StatCountResponse assists,
        StatCountResponse yellowCards,
        StatCountResponse redCards,
        ResultStatsResponse results,
        StatAverageResponse averageOverallRating,
        StatAverageResponse goalsPerMatch,
        StatAverageResponse assistsPerMatch
) {
}
