package com.scouting.scouting.player.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = false)
public record PlayerHistoryRatingsDto(
        @Min(value = 1, message = "El rating tecnico debe estar entre 1 y 5.")
        @Max(value = 5, message = "El rating tecnico debe estar entre 1 y 5.")
        Integer technical,
        @Min(value = 1, message = "El rating tactico debe estar entre 1 y 5.")
        @Max(value = 5, message = "El rating tactico debe estar entre 1 y 5.")
        Integer tactical,
        @Min(value = 1, message = "El rating fisico debe estar entre 1 y 5.")
        @Max(value = 5, message = "El rating fisico debe estar entre 1 y 5.")
        Integer physical,
        @Min(value = 1, message = "El rating mental debe estar entre 1 y 5.")
        @Max(value = 5, message = "El rating mental debe estar entre 1 y 5.")
        Integer mental,
        @Min(value = 1, message = "El rating general debe estar entre 1 y 5.")
        @Max(value = 5, message = "El rating general debe estar entre 1 y 5.")
        Integer overall
) {
}
