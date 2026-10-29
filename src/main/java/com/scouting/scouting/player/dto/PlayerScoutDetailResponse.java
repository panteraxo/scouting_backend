package com.scouting.scouting.player.dto;

import com.scouting.scouting.player.entity.DominantFoot;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PositionGroup;

import java.math.BigDecimal;

public record PlayerScoutDetailResponse(
        Long id,
        String firstName,
        String lastName,
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
        boolean guardianDataAvailable
) {
}
