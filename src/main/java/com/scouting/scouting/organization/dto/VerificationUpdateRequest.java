package com.scouting.scouting.organization.dto;

import com.scouting.scouting.organization.entity.VerificationStatus;
import jakarta.validation.constraints.NotNull;

/** Cambio de verificacion solicitado por ADMIN. */
public record VerificationUpdateRequest(
        @NotNull(message = "El estado de verificacion es obligatorio.")
        VerificationStatus status
) {
}
