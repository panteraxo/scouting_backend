package com.scouting.scouting.prospecting.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.prospecting.entity.ProspectStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProspectStatusUpdateRequest(

        @NotNull(message = "El estado es obligatorio.")
        ProspectStatus status,

        @NotNull(message = "La version es obligatoria.")
        @Min(value = 0, message = "La version no es valida.")
        Long version
) {
}
