package com.scouting.scouting.player.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.scouting.scouting.player.entity.DominantFoot;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PlayerProfileStatus;
import com.scouting.scouting.player.entity.PositionGroup;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlayerDirectoryItemResponse(
        Long id,
        String firstName,
        String lastName,
        Integer age,
        Integer yearsExperience,
        String photoUrl,
        PlayerPosition position,
        PositionGroup positionGroup,
        PlayerPosition bestPosition,
        PositionGroup bestPositionGroup,
        DominantFoot dominantFoot,
        Integer heightCm,
        BigDecimal weightKg,
        ResidenceResponse residence,
        AcademyRefResponse academy,
        PlayerOperationalStatus operationalStatus,
        PlayerProfileStatus profileStatus
) {
}
