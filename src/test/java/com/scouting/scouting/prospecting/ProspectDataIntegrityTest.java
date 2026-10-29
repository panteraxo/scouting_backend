package com.scouting.scouting.prospecting;

import com.scouting.scouting.prospecting.service.ProspectDataIntegrity;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProspectDataIntegrityTest {

    @Test
    void identifiesScoutPlayerUniqueViolation() {
        DataIntegrityViolationException unique = new DataIntegrityViolationException(
                "conflict", new RuntimeException("Unique index UK_PROSPECTS_SCOUT_PLAYER on SCOUT_ID, PLAYER_ID"));

        assertTrue(ProspectDataIntegrity.isScoutPlayerUniqueViolation(unique));
    }

    @Test
    void doesNotTreatOtherIntegrityViolationsAsDuplicateProspect() {
        DataIntegrityViolationException other = new DataIntegrityViolationException(
                "conflict", new RuntimeException("not-null constraint violated"));

        assertFalse(ProspectDataIntegrity.isScoutPlayerUniqueViolation(other));
    }
}
