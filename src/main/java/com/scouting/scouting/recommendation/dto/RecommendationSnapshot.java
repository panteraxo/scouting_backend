package com.scouting.scouting.recommendation.dto;

import com.scouting.scouting.player.dto.AcademyRefResponse;
import com.scouting.scouting.player.dto.PlayerHistoryMetricsDto;
import com.scouting.scouting.player.dto.PlayerHistoryRatingsDto;
import com.scouting.scouting.player.dto.ResidenceResponse;
import com.scouting.scouting.player.entity.ActivityType;
import com.scouting.scouting.player.entity.AgeCategory;
import com.scouting.scouting.player.entity.CompetitiveLevel;
import com.scouting.scouting.player.entity.DominantFoot;
import com.scouting.scouting.player.entity.Participation;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PositionGroup;
import com.scouting.scouting.player.entity.TeamResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Snapshot inmutable compartido al club. No contiene tutor, cuenta, fecha de
 * nacimiento, distrito ni notas privadas.
 */
public record RecommendationSnapshot(
        PlayerSnapshot player,
        ResidenceResponse residence,
        List<HistorySnapshot> historyEntries,
        List<EvaluationSnapshot> evaluations
) {

    public record PlayerSnapshot(
            String firstName,
            String lastName,
            Integer age,
            PlayerPosition position,
            PositionGroup positionGroup,
            PlayerPosition bestPosition,
            PositionGroup bestPositionGroup,
            DominantFoot dominantFoot,
            Integer heightCm,
            BigDecimal weightKg,
            String summary,
            String photoUrl,
            AcademyRefResponse academy
    ) {
    }

    public record HistorySnapshot(
            Long id,
            ActivityType activityType,
            LocalDate activityDate,
            String competition,
            String opponent,
            AgeCategory ageCategory,
            CompetitiveLevel competitiveLevel,
            PlayerPosition positionPlayed,
            PositionGroup positionGroup,
            Participation participation,
            Integer minutesPlayed,
            TeamResult teamResult,
            Integer teamGoals,
            Integer opponentGoals,
            Integer playerGoals,
            Integer assists,
            Integer yellowCards,
            Integer redCards,
            String observations,
            String videoUrl,
            Integer videoTimestampSeconds,
            String videoDescription,
            PlayerHistoryMetricsDto metrics,
            PlayerHistoryRatingsDto ratings,
            String strengths,
            String improvements,
            String recommendations
    ) {
    }

    public record EvaluationSnapshot(
            Long id,
            LocalDate evaluatedAt,
            Integer technicalRating,
            Integer tacticalRating,
            Integer physicalRating,
            Integer mentalRating,
            Integer overallRating,
            String strengths,
            String improvements,
            String recommendation,
            String videoUrl
    ) {
    }
}
