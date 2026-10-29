package com.scouting.scouting.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationCreateRequest(

        @NotNull(message = "El club es obligatorio.")
        @Positive(message = "El club no es valido.")
        Long clubId,

        @NotBlank(message = "La carta es obligatoria.")
        @Size(max = 4000, message = "La carta no debe superar 4000 caracteres.")
        String message,

        @Size(max = 100, message = "No se pueden seleccionar mas de 100 historiales.")
        List<@Positive(message = "El historial no es valido.") Long> historyEntryIds,

        @Size(max = 100, message = "No se pueden seleccionar mas de 100 evaluaciones.")
        List<@Positive(message = "La evaluacion no es valida.") Long> evaluationIds
) {
}
