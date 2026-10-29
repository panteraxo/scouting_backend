package com.scouting.scouting.player.dto;

import com.scouting.scouting.player.entity.ActivityType;
import com.scouting.scouting.player.entity.AgeCategory;
import com.scouting.scouting.player.entity.CompetitiveLevel;
import com.scouting.scouting.player.entity.Participation;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PositionGroup;
import com.scouting.scouting.player.entity.TeamResult;

import java.time.Instant;
import java.time.LocalDate;

public record PlayerHistoryEntryResponse(
        Long id,
        Long playerId,
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
        String recommendations,
        ActorSummaryResponse author,
        ActorSummaryResponse lastEditedBy,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        Integer revisionNumber
) {
}
