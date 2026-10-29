package com.scouting.scouting.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.recommendation.entity.RecommendationResponseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationClubReplyRequest(

        @NotNull(message = "El estado de la respuesta es obligatorio.")
        RecommendationResponseStatus status,

        @NotBlank(message = "El mensaje es obligatorio.")
        @Size(max = 4000, message = "El mensaje no debe superar 4000 caracteres.")
        String message
) {
}
