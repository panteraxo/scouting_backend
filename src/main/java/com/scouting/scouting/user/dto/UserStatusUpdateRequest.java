package com.scouting.scouting.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Cambio de vigencia de una cuenta existente. Solo se admiten ACTIVE e
 * INACTIVE; PENDING_APPROVAL se gestiona exclusivamente desde la revision de
 * solicitudes de ojeador.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserStatusUpdateRequest(

        @NotNull(message = "El estado es obligatorio.")
        UserStatus status
) {
}
