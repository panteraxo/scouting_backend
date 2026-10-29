package com.scouting.scouting.player.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.player.entity.ActivityType;
import com.scouting.scouting.player.entity.AgeCategory;
import com.scouting.scouting.player.entity.CompetitiveLevel;
import com.scouting.scouting.player.entity.Participation;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.TeamResult;
import com.scouting.scouting.validation.HttpUrl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerHistoryEntryCreateRequest(

        @NotNull(message = "El tipo de actividad es obligatorio.")
        ActivityType activityType,

        @NotNull(message = "La fecha de actividad es obligatoria.")
        LocalDate activityDate,

        @Size(max = 150, message = "El campeonato no debe superar 150 caracteres.")
        String competition,

        @Size(max = 150, message = "El rival no debe superar 150 caracteres.")
        String opponent,

        AgeCategory ageCategory,

        CompetitiveLevel competitiveLevel,

        PlayerPosition positionPlayed,

        Participation participation,

        @Min(value = 0, message = "Los minutos no pueden ser negativos.")
        @Max(value = 150, message = "Los minutos no deben superar 150.")
        Integer minutesPlayed,

        TeamResult teamResult,

        @Min(value = 0, message = "Los goles del equipo no pueden ser negativos.")
        @Max(value = 99, message = "Los goles del equipo no deben superar 99.")
        Integer teamGoals,

        @Min(value = 0, message = "Los goles del rival no pueden ser negativos.")
        @Max(value = 99, message = "Los goles del rival no deben superar 99.")
        Integer opponentGoals,

        @Min(value = 0, message = "Los goles del jugador no pueden ser negativos.")
        @Max(value = 99, message = "Los goles del jugador no deben superar 99.")
        Integer playerGoals,

        @Min(value = 0, message = "Las asistencias no pueden ser negativas.")
        @Max(value = 99, message = "Las asistencias no deben superar 99.")
        Integer assists,

        @Min(value = 0, message = "Las amarillas no pueden ser negativas.")
        @Max(value = 2, message = "Las amarillas no deben superar 2.")
        Integer yellowCards,

        @Min(value = 0, message = "Las rojas no pueden ser negativas.")
        @Max(value = 1, message = "Las rojas no deben superar 1.")
        Integer redCards,

        @Size(max = 2000, message = "Las observaciones no deben superar 2000 caracteres.")
        String observations,

        @HttpUrl
        @Size(max = 500, message = "El video no debe superar 500 caracteres.")
        String videoUrl,

        @Min(value = 0, message = "El timestamp no puede ser negativo.")
        @Max(value = 86400, message = "El timestamp no debe superar 86400 segundos.")
        Integer videoTimestampSeconds,

        @Size(max = 500, message = "La descripcion del video no debe superar 500 caracteres.")
        String videoDescription,

        @Valid
        PlayerHistoryMetricsDto metrics,

        @Valid
        PlayerHistoryRatingsDto ratings,

        @Size(max = 2000, message = "Las fortalezas no deben superar 2000 caracteres.")
        String strengths,

        @Size(max = 2000, message = "Las mejoras no deben superar 2000 caracteres.")
        String improvements,

        @Size(max = 2000, message = "Las recomendaciones no deben superar 2000 caracteres.")
        String recommendations
) {
}
