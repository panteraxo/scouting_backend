package com.scouting.scouting.player.entity;

/**
 * Tipos de actividad del historial deportivo. Solo los tres primeros cuentan
 * como partido para las estadisticas.
 */
public enum ActivityType {
    OFFICIAL_MATCH(true),
    FRIENDLY_MATCH(true),
    TOURNAMENT(true),
    TRAINING(false),
    SPORTS_TRIAL(false);

    private final boolean match;

    ActivityType(boolean match) {
        this.match = match;
    }

    public boolean isMatch() {
        return match;
    }
}
