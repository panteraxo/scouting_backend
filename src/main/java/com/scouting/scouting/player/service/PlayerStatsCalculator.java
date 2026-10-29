package com.scouting.scouting.player.service;

import com.scouting.scouting.player.dto.PlayerActivityTypeStatsResponse;
import com.scouting.scouting.player.dto.PlayerPositionGroupStatsResponse;
import com.scouting.scouting.player.dto.PlayerPositionStatsResponse;
import com.scouting.scouting.player.dto.PlayerRatingsResponse;
import com.scouting.scouting.player.dto.PlayerStatsResponse;
import com.scouting.scouting.player.dto.PlayerStatsTotalsResponse;
import com.scouting.scouting.player.dto.PlayerYearStatsResponse;
import com.scouting.scouting.player.dto.ResultStatsResponse;
import com.scouting.scouting.player.dto.StatAverageResponse;
import com.scouting.scouting.player.dto.StatCountResponse;
import com.scouting.scouting.player.entity.ActivityType;
import com.scouting.scouting.player.entity.Participation;
import com.scouting.scouting.player.entity.PlayerHistoryEntry;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PositionGroup;
import com.scouting.scouting.player.entity.TeamResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Calculo puro de estadisticas; preserva nulos y cobertura. */
public class PlayerStatsCalculator {

    public PlayerStatsResponse calculate(Long playerId, List<PlayerHistoryEntry> allEntries) {
        List<PlayerHistoryEntry> matches = allEntries.stream()
                .filter(entry -> entry.getActivityType().isMatch())
                .toList();

        PlayerStatsTotalsResponse career = totals(matches, true);

        Map<Integer, List<PlayerHistoryEntry>> byYear = matches.stream()
                .collect(Collectors.groupingBy(entry -> entry.getActivityDate().getYear(),
                        LinkedHashMap::new, Collectors.toList()));
        List<PlayerYearStatsResponse> years = byYear.entrySet().stream()
                .sorted(Map.Entry.<Integer, List<PlayerHistoryEntry>>comparingByKey().reversed())
                .map(entry -> new PlayerYearStatsResponse(entry.getKey(), totals(entry.getValue(), true)))
                .toList();

        List<PlayerActivityTypeStatsResponse> byActivityType = new ArrayList<>();
        for (ActivityType type : ActivityType.values()) {
            List<PlayerHistoryEntry> entries = allEntries.stream()
                    .filter(entry -> entry.getActivityType() == type)
                    .toList();
            byActivityType.add(new PlayerActivityTypeStatsResponse(
                    type, type.isMatch(), entries.size(), totals(entries, type.isMatch())));
        }

        Map<PlayerPosition, List<PlayerHistoryEntry>> byPosition = matches.stream()
                .filter(entry -> entry.getPositionPlayed() != null)
                .collect(Collectors.groupingBy(PlayerHistoryEntry::getPositionPlayed,
                        LinkedHashMap::new, Collectors.toList()));
        List<PlayerPositionStatsResponse> positions = new ArrayList<>();
        for (PlayerPosition position : PlayerPosition.values()) {
            List<PlayerHistoryEntry> entries = byPosition.get(position);
            if (entries != null && !entries.isEmpty()) {
                positions.add(new PlayerPositionStatsResponse(position, position.group(), totals(entries, true)));
            }
        }
        List<PlayerHistoryEntry> unassigned = matches.stream()
                .filter(entry -> entry.getPositionPlayed() == null)
                .toList();
        if (!unassigned.isEmpty()) {
            positions.add(new PlayerPositionStatsResponse(null, null, totals(unassigned, true)));
        }

        Map<PositionGroup, List<PlayerHistoryEntry>> byGroup = matches.stream()
                .filter(entry -> entry.getPositionPlayed() != null)
                .collect(Collectors.groupingBy(entry -> entry.getPositionPlayed().group(),
                        LinkedHashMap::new, Collectors.toList()));
        List<PlayerPositionGroupStatsResponse> groups = new ArrayList<>();
        for (PositionGroup group : PositionGroup.values()) {
            List<PlayerHistoryEntry> entries = byGroup.get(group);
            if (entries != null && !entries.isEmpty()) {
                groups.add(new PlayerPositionGroupStatsResponse(group, totals(entries, true)));
            }
        }
        List<PlayerHistoryEntry> unassignedGroup = matches.stream()
                .filter(entry -> entry.getPositionPlayed() == null)
                .toList();
        if (!unassignedGroup.isEmpty()) {
            groups.add(new PlayerPositionGroupStatsResponse(null, totals(unassignedGroup, true)));
        }

        PlayerRatingsResponse ratings = new PlayerRatingsResponse(
                average(matches, PlayerHistoryEntry::getTechnicalRating),
                average(matches, PlayerHistoryEntry::getTacticalRating),
                average(matches, PlayerHistoryEntry::getPhysicalRating),
                average(matches, PlayerHistoryEntry::getMentalRating),
                average(matches, PlayerHistoryEntry::getOverallRating));

        return new PlayerStatsResponse(playerId, career, years, byActivityType, positions, groups, ratings);
    }

    private PlayerStatsTotalsResponse totals(List<PlayerHistoryEntry> entries, boolean matchContext) {
        StatCountResponse matchesPlayed = matchContext
                ? matchesPlayed(entries)
                : new StatCountResponse(null, 0, entries.size(), false, null);
        return new PlayerStatsTotalsResponse(
                matchesPlayed,
                matchContext ? entries.size() : 0,
                count(entries, entry -> knownValue(entry, entry.getMinutesPlayed())),
                count(entries, entry -> knownValue(entry, entry.getPlayerGoals())),
                count(entries, entry -> knownValue(entry, entry.getAssists())),
                count(entries, entry -> knownValue(entry, entry.getYellowCards())),
                count(entries, entry -> knownValue(entry, entry.getRedCards())),
                matchContext && !entries.isEmpty() ? results(entries) : null,
                average(entries, PlayerHistoryEntry::getOverallRating),
                perMatch(entries, matchesPlayed, count(entries, entry -> knownValue(entry, entry.getPlayerGoals())), matchContext),
                perMatch(entries, matchesPlayed, count(entries, entry -> knownValue(entry, entry.getAssists())), matchContext));
    }

    private StatCountResponse matchesPlayed(List<PlayerHistoryEntry> entries) {
        int known = 0;
        int played = 0;
        for (PlayerHistoryEntry entry : entries) {
            if (entry.getParticipation() != null) {
                known++;
                if (entry.getParticipation() == Participation.STARTER
                        || entry.getParticipation() == Participation.SUBSTITUTE) {
                    played++;
                }
            }
        }
        return new StatCountResponse(
                known == 0 ? null : played,
                known,
                entries.size(),
                entries.size() > 0 && known == entries.size(),
                coverage(known, entries.size()));
    }

    private StatCountResponse count(List<PlayerHistoryEntry> entries,
                                    Function<PlayerHistoryEntry, Integer> extractor) {
        int known = 0;
        int sum = 0;
        for (PlayerHistoryEntry entry : entries) {
            Integer value = extractor.apply(entry);
            if (value != null) {
                known++;
                sum += value;
            }
        }
        return new StatCountResponse(
                known == 0 ? null : sum,
                known,
                entries.size(),
                entries.size() > 0 && known == entries.size(),
                coverage(known, entries.size()));
    }

    private StatAverageResponse average(List<PlayerHistoryEntry> entries,
                                        Function<PlayerHistoryEntry, Integer> extractor) {
        int known = 0;
        long sum = 0;
        for (PlayerHistoryEntry entry : entries) {
            Integer value = extractor.apply(entry);
            if (value != null) {
                known++;
                sum += value;
            }
        }
        BigDecimal value = known == 0 ? null
                : BigDecimal.valueOf(sum)
                        .divide(BigDecimal.valueOf(known), 2, RoundingMode.HALF_UP);
        return new StatAverageResponse(
                value,
                known,
                entries.size(),
                entries.size() > 0 && known == entries.size(),
                coverage(known, entries.size()));
    }

    private StatAverageResponse perMatch(List<PlayerHistoryEntry> entries,
                                         StatCountResponse matchesPlayed,
                                         StatCountResponse stat,
                                         boolean matchContext) {
        boolean complete = matchContext
                && matchesPlayed.value() != null
                && matchesPlayed.value() > 0
                && matchesPlayed.complete()
                && stat.complete();
        BigDecimal value = complete
                ? BigDecimal.valueOf(stat.value())
                        .divide(BigDecimal.valueOf(matchesPlayed.value()), 2, RoundingMode.HALF_UP)
                : null;
        return new StatAverageResponse(
                value,
                stat.knownEntries(),
                entries.size(),
                complete,
                coverage(stat.knownEntries(), entries.size()));
    }

    private ResultStatsResponse results(List<PlayerHistoryEntry> entries) {
        int known = 0;
        int wins = 0;
        int draws = 0;
        int losses = 0;
        for (PlayerHistoryEntry entry : entries) {
            TeamResult result = entry.getTeamResult();
            if (result == null) {
                continue;
            }
            known++;
            switch (result) {
                case WIN -> wins++;
                case DRAW -> draws++;
                case LOSS -> losses++;
            }
        }
        int total = entries.size();
        StatCountResponse winsStat = resultCount(wins, known, total);
        StatCountResponse drawsStat = resultCount(draws, known, total);
        StatCountResponse lossesStat = resultCount(losses, known, total);
        BigDecimal winRate = known == 0 ? null
                : BigDecimal.valueOf(wins).divide(BigDecimal.valueOf(known), 4, RoundingMode.HALF_UP);
        return new ResultStatsResponse(
                winsStat,
                drawsStat,
                lossesStat,
                total - known,
                new StatAverageResponse(
                        winRate,
                        known,
                        total,
                        total > 0 && known == total,
                        coverage(known, total)));
    }

    private StatCountResponse resultCount(int value, int known, int total) {
        return new StatCountResponse(
                known == 0 ? null : value,
                known,
                total,
                total > 0 && known == total,
                coverage(known, total));
    }

    private Integer knownValue(PlayerHistoryEntry entry, Integer value) {
        if (entry.getParticipation() == Participation.DID_NOT_PLAY) {
            return 0;
        }
        return value;
    }

    private BigDecimal coverage(int known, int total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(known)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }
}
