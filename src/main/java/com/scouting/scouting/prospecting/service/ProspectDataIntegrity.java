package com.scouting.scouting.prospecting.service;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;

/** Distingue la unique scout-player de otros conflictos de integridad. */
public final class ProspectDataIntegrity {

    private ProspectDataIntegrity() {
    }

    public static boolean isScoutPlayerUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause == null ? null : cause.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("uk_prospects_scout_player")
                || (normalized.contains("scout_id") && normalized.contains("player_id"));
    }
}
