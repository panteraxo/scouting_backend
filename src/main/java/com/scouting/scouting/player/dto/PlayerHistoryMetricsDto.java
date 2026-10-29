package com.scouting.scouting.player.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * Metricas posicionales exactas. Solo se acepta el grupo correspondiente a la
 * posicion jugada; los demas deben ser null.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record PlayerHistoryMetricsDto(
        @Valid Goalkeeper goalkeeper,
        @Valid Defender defender,
        @Valid Midfielder midfielder,
        @Valid Forward forward
) {

    @JsonAnySetter
    public void rejectUnknownGroup(String name, Object value) {
        throw new IllegalArgumentException("Metrica posicional desconocida: " + name);
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record Goalkeeper(
            @Min(value = 0, message = "Las atajadas no pueden ser negativas.") Integer saves,
            @Min(value = 0, message = "Los goles recibidos no pueden ser negativos.") Integer goalsConceded,
            Boolean cleanSheet,
            @Min(value = 0, message = "Las intervenciones no pueden ser negativas.") Integer crossInterventions,
            @Min(value = 0, message = "Los pases intentados no pueden ser negativos.") Integer passesAttempted,
            @Min(value = 0, message = "Los pases completados no pueden ser negativos.") Integer passesCompleted
    ) {
        @JsonAnySetter
        public void rejectUnknownMetric(String name, Object value) {
            throw new IllegalArgumentException("Metrica de arquero desconocida: " + name);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record Defender(
            @Min(value = 0, message = "Las entradas no pueden ser negativas.") Integer successfulTackles,
            @Min(value = 0, message = "Las intercepciones no pueden ser negativas.") Integer interceptions,
            @Min(value = 0, message = "Los despejes no pueden ser negativos.") Integer clearances,
            @Min(value = 0, message = "Los duelos ganados no pueden ser negativos.") Integer duelsWon,
            @Min(value = 0, message = "Los bloqueos no pueden ser negativos.") Integer blocks,
            @Min(value = 0, message = "Los errores no pueden ser negativos.") Integer errorsLeadingToChance
    ) {
        @JsonAnySetter
        public void rejectUnknownMetric(String name, Object value) {
            throw new IllegalArgumentException("Metrica de defensa desconocida: " + name);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record Midfielder(
            @Min(value = 0, message = "Los pases intentados no pueden ser negativos.") Integer passesAttempted,
            @Min(value = 0, message = "Los pases completados no pueden ser negativos.") Integer passesCompleted,
            @Min(value = 0, message = "Las intercepciones no pueden ser negativas.") Integer interceptions,
            @Min(value = 0, message = "Los pases clave no pueden ser negativos.") Integer keyPasses,
            @Min(value = 0, message = "Las asistencias no pueden ser negativas.") Integer assists,
            @Min(value = 0, message = "Las recuperaciones no pueden ser negativas.") Integer recoveries,
            @Min(value = 0, message = "Los duelos ganados no pueden ser negativos.") Integer duelsWon,
            @Min(value = 0, message = "Los regates no pueden ser negativos.") Integer successfulDribbles,
            @Min(value = 0, message = "Los tiros no pueden ser negativos.") Integer shots
    ) {
        @JsonAnySetter
        public void rejectUnknownMetric(String name, Object value) {
            throw new IllegalArgumentException("Metrica de mediocampo desconocida: " + name);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record Forward(
            @Min(value = 0, message = "Los goles no pueden ser negativos.") Integer goals,
            @Min(value = 0, message = "Las asistencias no pueden ser negativas.") Integer assists,
            @Min(value = 0, message = "Los tiros no pueden ser negativos.") Integer shots,
            @Min(value = 0, message = "Los regates no pueden ser negativos.") Integer successfulDribbles,
            @Min(value = 0, message = "Las ocasiones creadas no pueden ser negativas.") Integer chancesCreated,
            @Min(value = 0, message = "Los centros intentados no pueden ser negativos.") Integer crossesAttempted,
            @Min(value = 0, message = "Los centros completados no pueden ser negativos.") Integer crossesCompleted
    ) {
        @JsonAnySetter
        public void rejectUnknownMetric(String name, Object value) {
            throw new IllegalArgumentException("Metrica de delantero desconocida: " + name);
        }
    }
}
