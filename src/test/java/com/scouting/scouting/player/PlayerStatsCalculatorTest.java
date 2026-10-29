package com.scouting.scouting.player;

import com.scouting.scouting.player.dto.PlayerStatsResponse;
import com.scouting.scouting.player.dto.PlayerStatsTotalsResponse;
import com.scouting.scouting.player.entity.ActivityType;
import com.scouting.scouting.player.entity.Participation;
import com.scouting.scouting.player.entity.PlayerHistoryEntry;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.TeamResult;
import com.scouting.scouting.player.service.PlayerStatsCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStatsCalculatorTest {

    private final PlayerStatsCalculator calculator = new PlayerStatsCalculator();

    @Test
    void preservesNullsAndCoverage() {
        PlayerHistoryEntry a = entry(ActivityType.OFFICIAL_MATCH, "2026-01-10", Participation.STARTER,
                TeamResult.WIN, PlayerPosition.STRIKER);
        a.setMinutesPlayed(90);
        a.setPlayerGoals(1);
        a.setOverallRating(4);
        PlayerHistoryEntry b = entry(ActivityType.OFFICIAL_MATCH, "2026-02-10", Participation.SUBSTITUTE,
                TeamResult.DRAW, PlayerPosition.STRIKER);
        b.setMinutesPlayed(20);
        PlayerHistoryEntry c = entry(ActivityType.OFFICIAL_MATCH, "2026-03-10", Participation.DID_NOT_PLAY,
                TeamResult.LOSS, PlayerPosition.STRIKER);

        PlayerStatsResponse response = calculator.calculate(1L, List.of(a, b, c));
        PlayerStatsTotalsResponse career = response.career();

        assertEquals(2, career.matchesPlayed().value());
        assertTrue(career.matchesPlayed().complete());
        assertEquals(110, career.minutes().value());
        assertEquals(3, career.minutes().knownEntries());
        assertTrue(career.minutes().complete());
        assertEquals(1, career.goals().value());
        assertEquals(2, career.goals().knownEntries());
        assertEquals(4, career.averageOverallRating().value().intValue());
        assertEquals(1, career.averageOverallRating().knownEntries());
        assertNull(career.goalsPerMatch().value());
    }

    @Test
    void trainingAndTrialDoNotCountAsMatches() {
        PlayerHistoryEntry training = entry(ActivityType.TRAINING, "2026-01-10", null, null, null);
        training.setMinutesPlayed(60);
        PlayerHistoryEntry trial = entry(ActivityType.SPORTS_TRIAL, "2026-01-11", null, null, null);
        trial.setMinutesPlayed(30);

        PlayerStatsResponse response = calculator.calculate(1L, List.of(training, trial));

        assertEquals(0, response.career().matchesRecorded());
        assertNull(response.career().matchesPlayed().value());
        assertNull(response.career().results());
        var trainingStats = response.byActivityType().stream()
                .filter(item -> item.activityType() == ActivityType.TRAINING)
                .findFirst().orElseThrow();
        assertFalse(trainingStats.matchLike());
        assertEquals(1, trainingStats.entries());
        assertNull(trainingStats.totals().matchesPlayed().value());
        assertNull(trainingStats.totals().results());
    }

    @Test
    void resultsAndWinRateUseExplicitTeamResult() {
        PlayerHistoryEntry win = entry(ActivityType.OFFICIAL_MATCH, "2026-01-10", Participation.STARTER,
                TeamResult.WIN, PlayerPosition.STRIKER);
        PlayerHistoryEntry draw = entry(ActivityType.FRIENDLY_MATCH, "2026-01-11", Participation.STARTER,
                TeamResult.DRAW, PlayerPosition.STRIKER);
        PlayerHistoryEntry loss = entry(ActivityType.TOURNAMENT, "2026-01-12", Participation.STARTER,
                TeamResult.LOSS, PlayerPosition.STRIKER);
        PlayerHistoryEntry unknown = entry(ActivityType.OFFICIAL_MATCH, "2026-01-13", Participation.STARTER,
                null, PlayerPosition.STRIKER);

        PlayerStatsResponse response = calculator.calculate(1L, List.of(win, draw, loss, unknown));
        var results = response.career().results();

        assertEquals(1, results.wins().value());
        assertEquals(1, results.draws().value());
        assertEquals(1, results.losses().value());
        assertEquals(1, results.unknownResults());
        assertEquals(0, results.winRate().value().compareTo(new java.math.BigDecimal("0.3333")));
    }

    @Test
    void groupsByYearPositionAndPositionGroup() {
        PlayerHistoryEntry a = entry(ActivityType.OFFICIAL_MATCH, "2025-01-10", Participation.STARTER,
                TeamResult.WIN, PlayerPosition.CENTER_BACK);
        PlayerHistoryEntry b = entry(ActivityType.OFFICIAL_MATCH, "2026-01-10", Participation.STARTER,
                TeamResult.WIN, PlayerPosition.CENTER_BACK);
        PlayerHistoryEntry c = entry(ActivityType.OFFICIAL_MATCH, "2026-02-10", Participation.STARTER,
                TeamResult.WIN, PlayerPosition.STRIKER);

        PlayerStatsResponse response = calculator.calculate(1L, List.of(a, b, c));

        assertEquals(2, response.byYear().size());
        assertEquals(2, response.byPosition().size());
        assertEquals(2, response.byPositionGroup().size());
    }

    private PlayerHistoryEntry entry(ActivityType type, String date, Participation participation,
                                     TeamResult result, PlayerPosition position) {
        PlayerHistoryEntry entry = new PlayerHistoryEntry();
        entry.setActivityType(type);
        entry.setActivityDate(LocalDate.parse(date));
        entry.setParticipation(participation);
        entry.setTeamResult(result);
        entry.setPositionPlayed(position);
        return entry;
    }
}
