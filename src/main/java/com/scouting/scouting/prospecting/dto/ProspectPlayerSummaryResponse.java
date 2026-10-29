package com.scouting.scouting.prospecting.dto;

import com.scouting.scouting.player.dto.AcademyRefResponse;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PositionGroup;

/** Resumen deportivo del jugador visible para el ojeador autor. */
public record ProspectPlayerSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String photoUrl,
        Integer age,
        PlayerPosition position,
        PositionGroup positionGroup,
        AcademyRefResponse academy
) {
}
