package com.scouting.scouting.player.dto;

import java.math.BigDecimal;

public record StatCountResponse(
        Integer value,
        int knownEntries,
        int totalEntries,
        boolean complete,
        BigDecimal coverage
) {
}
