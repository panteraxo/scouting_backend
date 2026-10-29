package com.scouting.scouting.player.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.player.entity.GuardianConsentStatus;
import com.scouting.scouting.validation.HttpUrl;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GuardianConsentUpdateRequest(

        @NotNull(message = "El estado del consentimiento es obligatorio.")
        GuardianConsentStatus status,

        @HttpUrl
        @Size(max = 500, message = "La evidencia no debe superar 500 caracteres.")
        String evidenceUrl,

        @Size(max = 500, message = "La nota no debe superar 500 caracteres.")
        String note
) {
}
