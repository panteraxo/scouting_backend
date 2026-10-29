package com.scouting.scouting.recommendation;

import com.scouting.scouting.recommendation.service.RecommendationDataIntegrity;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationDataIntegrityTest {

    @Test
    void identifiesProspectClubUniqueViolation() {
        DataIntegrityViolationException unique = new DataIntegrityViolationException(
                "conflict", new RuntimeException("Unique index UK_RECOMMENDATIONS_PROSPECT_CLUB on PROSPECT_ID, CLUB_ID"));

        assertTrue(RecommendationDataIntegrity.isProspectClubUniqueViolation(unique));
    }

    @Test
    void doesNotTreatOtherConstraintsAsDuplicateRecommendation() {
        DataIntegrityViolationException other = new DataIntegrityViolationException(
                "conflict", new RuntimeException("not-null constraint violated"));

        assertFalse(RecommendationDataIntegrity.isProspectClubUniqueViolation(other));
    }
}
