package com.scouting.scouting.recommendation.service;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;

/** Distingue la unique (prospect, club) de otros conflictos de integridad. */
public final class RecommendationDataIntegrity {

    private RecommendationDataIntegrity() {
    }

    public static boolean isProspectClubUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause == null ? null : cause.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("uk_recommendations_prospect_club")
                || (normalized.contains("prospect_id") && normalized.contains("club_id"));
    }
}
