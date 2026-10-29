package com.scouting.scouting.prospecting.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.validation.HttpUrl;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProspectEvaluationCreateRequest(

        @NotNull(message = "La fecha de evaluacion es obligatoria.")
        LocalDate evaluatedAt,

        @NotNull(message = "El rating tecnico es obligatorio.")
        @Min(value = 1, message = "El rating tecnico debe estar entre 1 y 5.")
        @Max(value = 5, message = "El rating tecnico debe estar entre 1 y 5.")
        Integer technicalRating,

        @NotNull(message = "El rating tactico es obligatorio.")
        @Min(value = 1, message = "El rating tactico debe estar entre 1 y 5.")
        @Max(value = 5, message = "El rating tactico debe estar entre 1 y 5.")
        Integer tacticalRating,

        @NotNull(message = "El rating fisico es obligatorio.")
        @Min(value = 1, message = "El rating fisico debe estar entre 1 y 5.")
        @Max(value = 5, message = "El rating fisico debe estar entre 1 y 5.")
        Integer physicalRating,

        @NotNull(message = "El rating mental es obligatorio.")
        @Min(value = 1, message = "El rating mental debe estar entre 1 y 5.")
        @Max(value = 5, message = "El rating mental debe estar entre 1 y 5.")
        Integer mentalRating,

        @Min(value = 1, message = "El rating general debe estar entre 1 y 5.")
        @Max(value = 5, message = "El rating general debe estar entre 1 y 5.")
        Integer overallRating,

        @Size(max = 2000, message = "Las fortalezas no deben superar 2000 caracteres.")
        String strengths,

        @Size(max = 2000, message = "Las mejoras no deben superar 2000 caracteres.")
        String improvements,

        @Size(max = 2000, message = "La recomendacion no debe superar 2000 caracteres.")
        String recommendation,

        @Size(max = 2000, message = "Las notas privadas no deben superar 2000 caracteres.")
        String privateNotes,

        @HttpUrl
        @Size(max = 500, message = "El video no debe superar 500 caracteres.")
        String videoUrl
) {
}
