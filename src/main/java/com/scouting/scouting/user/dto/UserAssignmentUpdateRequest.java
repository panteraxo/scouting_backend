package com.scouting.scouting.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.user.entity.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cambio atomico de rol y organizacion. La organizacion debe corresponder al
 * rol: academia para PROFESSOR/ACADEMY_DIRECTOR/PLAYER, club para CLUB_MANAGER y
 * ninguna para ADMIN/SCOUT. {@code position} solo aplica a CLUB_MANAGER.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserAssignmentUpdateRequest(

        @NotNull(message = "El rol es obligatorio.")
        Role role,

        @Positive(message = "La academia no es valida.")
        Long academyId,

        @Positive(message = "El club no es valido.")
        Long clubId,

        @Size(max = 100, message = "El cargo no debe superar 100 caracteres.")
        String position
) {
}
