package com.scouting.scouting.player.dto;

import com.scouting.scouting.player.entity.DominantFoot;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PlayerProfileStatus;
import com.scouting.scouting.player.entity.PositionGroup;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PlayerAdminDetailResponse(
        Long id,
        Long userId,
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
        ActorSummaryResponse createdBy,
        ActorSummaryResponse updatedBy,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
