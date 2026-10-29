package com.scouting.scouting.recommendation.dto;

import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.VerificationStatus;

/** Referencia de club sin datos de contacto ni representante. */
public record RecommendationClubRefResponse(
        Long id,
        String code,
        String name,
        String logoUrl,
        OperationalStatus operationalStatus,
        VerificationStatus verificationStatus
) {
}
