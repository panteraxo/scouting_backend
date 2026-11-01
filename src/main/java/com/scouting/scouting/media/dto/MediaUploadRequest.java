package com.scouting.scouting.media.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.media.entity.MediaKind;
import com.scouting.scouting.media.entity.MediaPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaUploadRequest(

        @NotNull(message = "El tipo de archivo es obligatorio.")
        MediaKind kind,

        @NotNull(message = "El uso del archivo es obligatorio.")
        MediaPurpose purpose,

        @NotBlank(message = "El tipo de contenido es obligatorio.")
        @Size(max = 100, message = "El tipo de contenido no es valido.")
        String contentType,

        @NotNull(message = "El tamano es obligatorio.")
        @Positive(message = "El tamano debe ser mayor que cero.")
        Long sizeBytes
) {
}
