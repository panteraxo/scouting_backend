package com.scouting.scouting.prospecting.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ProspectEvaluationResponse(
        Long id,
        Long prospectId,
        LocalDate evaluatedAt,
        Integer technicalRating,
        Integer tacticalRating,
        Integer physicalRating,
        Integer mentalRating,
        Integer overallRating,
        String strengths,
        String improvements,
        String recommendation,
        String privateNotes,
        String videoUrl,
        Instant createdAt
) {
}
