package com.scouting.scouting.prospecting.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProspectCreateRequest(

        @NotNull(message = "El jugador es obligatorio.")
        @Positive(message = "El jugador no es valido.")
        Long playerId,

        @Size(max = 2000, message = "Las notas privadas no deben superar 2000 caracteres.")
        String privateNotes
) {
}
