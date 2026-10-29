package com.scouting.scouting.prospecting.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProspectNotesUpdateRequest(

        @Size(max = 2000, message = "Las notas privadas no deben superar 2000 caracteres.")
        String privateNotes,

        @NotNull(message = "La version es obligatoria.")
        @Min(value = 0, message = "La version no es valida.")
        Long version
) {
}
