package com.scouting.scouting.player.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerStatusUpdateRequest(
        @NotNull(message = "El estado es obligatorio.")
        PlayerOperationalStatus status
) {
}
