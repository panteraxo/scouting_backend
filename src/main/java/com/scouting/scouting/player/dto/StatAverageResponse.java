package com.scouting.scouting.player.dto;

import java.math.BigDecimal;

public record StatAverageResponse(
        BigDecimal value,
        int knownEntries,
        int totalEntries,
        boolean complete,
        BigDecimal coverage
) {
}
