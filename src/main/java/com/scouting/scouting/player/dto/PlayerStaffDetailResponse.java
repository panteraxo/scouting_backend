package com.scouting.scouting.player.dto;

import com.scouting.scouting.player.entity.DominantFoot;
import com.scouting.scouting.player.entity.GuardianConsentStatus;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PlayerProfileStatus;
import com.scouting.scouting.player.entity.PositionGroup;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PlayerStaffDetailResponse(
        Long id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        Integer age,
        Integer yearsExperience,
        Integer heightCm,
        BigDecimal weightKg,
        PlayerPosition position,
        PositionGroup positionGroup,
        PlayerPosition bestPosition,
        PositionGroup bestPositionGroup,
        DominantFoot dominantFoot,
        String summary,
        String photoUrl,
        ResidenceResponse residence,
        AcademyRefResponse academy,
        PlayerOperationalStatus operationalStatus,
        PlayerProfileStatus profileStatus,
        boolean guardianRequired,
        GuardianDataStatus guardianDataStatus,
        GuardianConsentStatus guardianConsentStatus,
        ActorSummaryResponse createdBy,
        ActorSummaryResponse updatedBy,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public enum GuardianDataStatus {
        NOT_APPLICABLE,
        MISSING,
        RECORDED
    }
}
