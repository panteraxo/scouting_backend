package com.scouting.scouting.player.service;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.player.dto.PlayerHistoryEntryCorrectionRequest;
import com.scouting.scouting.player.dto.PlayerHistoryEntryCreateRequest;
import com.scouting.scouting.player.dto.PlayerHistoryEntryResponse;
import com.scouting.scouting.player.dto.PlayerHistoryMetricsDto;
import com.scouting.scouting.player.dto.PlayerHistoryRatingsDto;
import com.scouting.scouting.player.dto.PlayerHistoryRevisionResponse;
import com.scouting.scouting.player.entity.ActivityType;
import com.scouting.scouting.player.entity.AgeCategory;
import com.scouting.scouting.player.entity.CompetitiveLevel;
import com.scouting.scouting.player.entity.HistoryChangeType;
import com.scouting.scouting.player.entity.Participation;
import com.scouting.scouting.player.entity.PlayerHistoryEntry;
import com.scouting.scouting.player.entity.PlayerHistoryMetric;
import com.scouting.scouting.player.entity.PlayerHistoryRevision;
import com.scouting.scouting.player.entity.PlayerMetricCode;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.entity.PositionGroup;
import com.scouting.scouting.player.entity.TeamResult;
import com.scouting.scouting.player.repository.PlayerHistoryEntryRepository;
import com.scouting.scouting.player.repository.PlayerHistoryRevisionRepository;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Historial deportivo append-only, correcciones auditadas y revisiones. */
@Service
public class PlayerHistoryService {

    private static final Set<String> SORT_PROPERTIES = Set.of(
            "activityDate", "createdAt", "updatedAt", "overallRating");

    private final PlayerHistoryEntryRepository entryRepository;
    private final PlayerHistoryRevisionRepository revisionRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerAccessService accessService;
    private final PlayerMapper mapper;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public PlayerHistoryService(PlayerHistoryEntryRepository entryRepository,
                                PlayerHistoryRevisionRepository revisionRepository,
                                PlayerProfileRepository playerProfileRepository,
                                PlayerAccessService accessService,
                                PlayerMapper mapper,
                                UserRepository userRepository,
                                ObjectMapper objectMapper) {
        this.entryRepository = entryRepository;
        this.revisionRepository = revisionRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.accessService = accessService;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<PlayerHistoryEntryResponse> list(AppUserPrincipal actor,
                                                         Long playerId,
                                                         int page,
                                                         int size,
                                                         String sort,
                                                         ActivityType activityType,
                                                         CompetitiveLevel competitiveLevel,
                                                         AgeCategory ageCategory,
                                                         PlayerPosition position,
                                                         Integer year,
                                                         LocalDate fromDate,
                                                         LocalDate toDate) {
        accessService.requireView(actor, playerId);
        validateDates(fromDate, toDate);
        Pageable pageable = pageable(page, size, sort);
        Page<PlayerHistoryEntry> result = entryRepository.search(
                playerId, activityType, competitiveLevel, ageCategory, position, year, fromDate, toDate, pageable);
        return PageResponse.from(result.map(mapper::toHistory));
    }

    @Transactional(readOnly = true)
    public PlayerHistoryEntryResponse get(AppUserPrincipal actor, Long playerId, Long historyId) {
        accessService.requireView(actor, playerId);
        PlayerHistoryEntry entry = entryRepository.findByIdAndPlayerProfile_Id(historyId, playerId)
                .orElseThrow(this::notFound);
        return mapper.toHistory(entry);
    }

    @Transactional
    public PlayerHistoryEntryResponse create(AppUserPrincipal actor, Long playerId,
                                             PlayerHistoryEntryCreateRequest request) {
        accessService.requireManage(actor, playerId);
        PlayerProfile profile = lockProfile(playerId);
        accessService.requireWritableByStaff(actor, profile);
        HistoryData data = HistoryData.from(request);
        validate(profile, data);

        PlayerHistoryEntry entry = new PlayerHistoryEntry();
        entry.setPlayerProfile(profile);
        entry.setRevisionNumber(1);
        entry.setCreatedBy(operator(actor));
        entry.setLastModifiedBy(operator(actor));
        apply(entry, data);
        PlayerHistoryEntry saved = entryRepository.saveAndFlush(entry);
        PlayerHistoryEntryResponse response = mapper.toHistory(saved);
        saveRevision(saved, HistoryChangeType.CREATED, null, response);
        return response;
    }

    @Transactional
    public PlayerHistoryEntryResponse correct(AppUserPrincipal actor, Long playerId, Long historyId,
                                              PlayerHistoryEntryCorrectionRequest request) {
        accessService.requireManage(actor, playerId);
        PlayerProfile profile = lockProfile(playerId);
        accessService.requireWritableByStaff(actor, profile);
        PlayerHistoryEntry entry = entryRepository.findByIdAndPlayerProfile_IdForUpdate(historyId, playerId)
                .orElseThrow(this::notFound);
        if (!entry.getVersion().equals(request.expectedVersion())) {
            throw ApiException.conflict("STALE_HISTORY_ENTRY",
                    "La entrada fue modificada por otra operacion.");
        }

        HistoryData data = HistoryData.from(request);
        validate(profile, data);
        entry.setLastModifiedBy(operator(actor));
        entry.setRevisionNumber(entry.getRevisionNumber() + 1);
        apply(entry, data);
        PlayerHistoryEntry saved = entryRepository.saveAndFlush(entry);
        PlayerHistoryEntryResponse response = mapper.toHistory(saved);
        saveRevision(saved, HistoryChangeType.CORRECTED, request.correctionReason(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<PlayerHistoryRevisionResponse> revisions(AppUserPrincipal actor, Long playerId,
                                                                 Long historyId, int page, int size) {
        accessService.requireManage(actor, playerId);
        entryRepository.findByIdAndPlayerProfile_Id(historyId, playerId).orElseThrow(this::notFound);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "revisionNumber"));
        Page<PlayerHistoryRevision> result = revisionRepository.findByHistoryEntry_Id(historyId, pageable);
        return PageResponse.from(result.map(revision -> new PlayerHistoryRevisionResponse(
                revision.getRevisionNumber(),
                revision.getChangeType(),
                revision.getReason(),
                mapper.actor(revision.getEditedBy()),
                revision.getEditedAt(),
                readSnapshot(revision.getSnapshotJson()))));
    }

    private void validate(PlayerProfile profile, HistoryData data) {
        LocalDate today = PlayerTime.today();
        if (data.activityDate().isAfter(today)) {
            throw ApiException.badRequest("INVALID_ACTIVITY_DATE", "La fecha de actividad no puede ser futura.");
        }
        if (data.activityDate().isBefore(profile.getBirthDate())) {
            throw ApiException.badRequest("INVALID_ACTIVITY_DATE",
                    "La fecha de actividad no puede ser anterior al nacimiento.");
        }
        if (data.videoTimestampSeconds() != null && data.videoUrl() == null) {
            throw ApiException.badRequest("INVALID_REQUEST", "El timestamp requiere una URL de video.");
        }
        if (data.videoDescription() != null && data.videoUrl() == null) {
            throw ApiException.badRequest("INVALID_REQUEST", "La descripcion requiere una URL de video.");
        }

        if (data.activityType().isMatch()) {
            if (data.teamResult() == null) {
                throw ApiException.badRequest("TEAM_RESULT_REQUIRED", "El resultado del equipo es obligatorio.");
            }
            if (data.opponent() == null || data.ageCategory() == null
                    || data.competitiveLevel() == null || data.positionPlayed() == null) {
                throw ApiException.badRequest("VALIDATION_ERROR",
                        "El rival, categoria, nivel y posicion son obligatorios en un partido.");
            }
            if ((data.teamGoals() == null) != (data.opponentGoals() == null)) {
                throw ApiException.badRequest("INCOMPLETE_SCORE", "Debe registrar ambos marcadores o ninguno.");
            }
            if (data.teamGoals() != null) {
                TeamResult expected = expectedResult(data.teamGoals(), data.opponentGoals());
                if (expected != data.teamResult()) {
                    throw ApiException.badRequest("RESULT_SCORE_MISMATCH",
                            "El resultado no coincide con el marcador.");
                }
            }
        } else {
            if (data.teamResult() != null || data.teamGoals() != null || data.opponentGoals() != null) {
                throw ApiException.badRequest("RESULT_NOT_APPLICABLE",
                        "Entrenamiento y prueba deportiva no admiten resultado ni marcador.");
            }
        }

        if (data.teamGoals() != null && data.playerGoals() != null && data.playerGoals() > data.teamGoals()) {
            throw ApiException.badRequest("INVALID_HISTORY_STAT",
                    "Los goles del jugador no pueden superar los goles del equipo.");
        }
        if (data.teamGoals() != null && data.assists() != null && data.assists() > data.teamGoals()) {
            throw ApiException.badRequest("INVALID_HISTORY_STAT",
                    "Las asistencias no pueden superar los goles del equipo.");
        }
        if (data.minutesPlayed() != null && data.minutesPlayed() > 0
                && data.participation() != Participation.STARTER
                && data.participation() != Participation.SUBSTITUTE) {
            throw ApiException.badRequest("INVALID_HISTORY_STAT",
                    "Los minutos requieren participacion como titular o suplente.");
        }
        if (data.participation() == Participation.DID_NOT_PLAY) {
            if (nonZero(data.minutesPlayed()) || nonZero(data.playerGoals()) || nonZero(data.assists())
                    || nonZero(data.yellowCards()) || nonZero(data.redCards())) {
                throw ApiException.badRequest("INVALID_HISTORY_STAT",
                        "Un jugador que no jugo no puede registrar minutos, goles, asistencias ni tarjetas.");
            }
            if (hasAnyMetrics(data.metrics())) {
                throw ApiException.badRequest("INVALID_HISTORY_STAT",
                        "Un jugador que no jugo no puede registrar metricas posicionales.");
            }
        }
        validateMetrics(data, profile);
    }

    private void validateMetrics(HistoryData data, PlayerProfile profile) {
        PlayerHistoryMetricsDto metrics = data.metrics();
        if (metrics == null) {
            return;
        }
        boolean any = metrics.goalkeeper() != null || metrics.defender() != null
                || metrics.midfielder() != null || metrics.forward() != null;
        if (!any) {
            return;
        }
        if (data.positionPlayed() == null) {
            throw ApiException.badRequest("INVALID_POSITION_METRICS",
                    "Las metricas requieren una posicion jugada.");
        }
        PositionGroup group = data.positionPlayed().group();
        if ((group == PositionGroup.GOALKEEPER && (metrics.defender() != null
                || metrics.midfielder() != null || metrics.forward() != null))
                || (group == PositionGroup.DEFENDER && (metrics.goalkeeper() != null
                || metrics.midfielder() != null || metrics.forward() != null))
                || (group == PositionGroup.MIDFIELDER && (metrics.goalkeeper() != null
                || metrics.defender() != null || metrics.forward() != null))
                || (group == PositionGroup.FORWARD && (metrics.goalkeeper() != null
                || metrics.defender() != null || metrics.midfielder() != null))) {
            throw ApiException.badRequest("INVALID_POSITION_METRICS",
                    "Las metricas no corresponden al grupo de la posicion jugada.");
        }

        validatePasses(group, metrics);
        validateCrosses(metrics);
        validateGoalkeeperConsistency(metrics, data);
        validateGoalsAssistsConsistency(metrics, data);
    }

    private void validatePasses(PositionGroup group, PlayerHistoryMetricsDto metrics) {
        if (group == PositionGroup.GOALKEEPER && metrics.goalkeeper() != null
                && metrics.goalkeeper().passesAttempted() != null
                && metrics.goalkeeper().passesCompleted() != null
                && metrics.goalkeeper().passesCompleted() > metrics.goalkeeper().passesAttempted()) {
            throw ApiException.badRequest("METRIC_STAT_MISMATCH",
                    "Los pases completados no pueden superar los intentados.");
        }
        if (group == PositionGroup.MIDFIELDER && metrics.midfielder() != null
                && metrics.midfielder().passesAttempted() != null
                && metrics.midfielder().passesCompleted() != null
                && metrics.midfielder().passesCompleted() > metrics.midfielder().passesAttempted()) {
            throw ApiException.badRequest("METRIC_STAT_MISMATCH",
                    "Los pases completados no pueden superar los intentados.");
        }
    }

    private void validateCrosses(PlayerHistoryMetricsDto metrics) {
        if (metrics.forward() != null
                && metrics.forward().crossesAttempted() != null
                && metrics.forward().crossesCompleted() != null
                && metrics.forward().crossesCompleted() > metrics.forward().crossesAttempted()) {
            throw ApiException.badRequest("METRIC_STAT_MISMATCH",
                    "Los centros completados no pueden superar los intentados.");
        }
    }

    private void validateGoalkeeperConsistency(PlayerHistoryMetricsDto metrics, HistoryData data) {
        if (metrics.goalkeeper() == null) {
            return;
        }
        Integer conceded = metrics.goalkeeper().goalsConceded();
        if (conceded != null && data.opponentGoals() != null && !conceded.equals(data.opponentGoals())) {
            throw ApiException.badRequest("METRIC_STAT_MISMATCH",
                    "Los goles recibidos no coinciden con los goles del rival.");
        }
        if (conceded != null && metrics.goalkeeper().cleanSheet() != null
                && metrics.goalkeeper().cleanSheet() != (conceded == 0)) {
            throw ApiException.badRequest("METRIC_STAT_MISMATCH",
                    "La valla invicta no coincide con los goles recibidos.");
        }
    }

    private void validateGoalsAssistsConsistency(PlayerHistoryMetricsDto metrics, HistoryData data) {
        if (metrics.forward() != null) {
            if (metrics.forward().goals() != null && data.playerGoals() != null
                    && !metrics.forward().goals().equals(data.playerGoals())) {
                throw ApiException.badRequest("METRIC_STAT_MISMATCH",
                        "Los goles posicionales no coinciden con los goles generales.");
            }
            if (metrics.forward().assists() != null && data.assists() != null
                    && !metrics.forward().assists().equals(data.assists())) {
                throw ApiException.badRequest("METRIC_STAT_MISMATCH",
                        "Las asistencias posicionales no coinciden con las generales.");
            }
        }
        if (metrics.midfielder() != null && metrics.midfielder().assists() != null
                && data.assists() != null
                && !metrics.midfielder().assists().equals(data.assists())) {
            throw ApiException.badRequest("METRIC_STAT_MISMATCH",
                    "Las asistencias posicionales no coinciden con las generales.");
        }
    }

    private void apply(PlayerHistoryEntry entry, HistoryData data) {
        entry.setActivityType(data.activityType());
        entry.setActivityDate(data.activityDate());
        entry.setCompetition(data.competition());
        entry.setOpponent(data.opponent());
        entry.setAgeCategory(data.ageCategory());
        entry.setCompetitiveLevel(data.competitiveLevel());
        entry.setPositionPlayed(data.positionPlayed());
        entry.setParticipation(data.participation());
        entry.setTeamResult(data.teamResult());
        entry.setTeamGoals(data.teamGoals());
        entry.setOpponentGoals(data.opponentGoals());

        if (data.participation() == Participation.DID_NOT_PLAY) {
            entry.setMinutesPlayed(0);
            entry.setPlayerGoals(0);
            entry.setAssists(0);
            entry.setYellowCards(0);
            entry.setRedCards(0);
        } else {
            entry.setMinutesPlayed(data.minutesPlayed());
            entry.setPlayerGoals(data.playerGoals());
            entry.setAssists(data.assists());
            entry.setYellowCards(data.yellowCards());
            entry.setRedCards(data.redCards());
        }

        entry.setObservations(data.observations());
        entry.setVideoUrl(data.videoUrl());
        entry.setVideoTimestampSeconds(data.videoTimestampSeconds());
        entry.setVideoDescription(data.videoDescription());
        entry.setTechnicalRating(data.ratings() == null ? null : data.ratings().technical());
        entry.setTacticalRating(data.ratings() == null ? null : data.ratings().tactical());
        entry.setPhysicalRating(data.ratings() == null ? null : data.ratings().physical());
        entry.setMentalRating(data.ratings() == null ? null : data.ratings().mental());
        entry.setOverallRating(data.ratings() == null ? null : data.ratings().overall());
        entry.setStrengths(data.strengths());
        entry.setImprovements(data.improvements());
        entry.setRecommendations(data.recommendations());

        entry.getMetrics().clear();
        if (data.metrics() != null && data.positionPlayed() != null) {
            entry.getMetrics().addAll(buildMetrics(entry, data.metrics(), data.positionPlayed().group()));
        }
    }

    private List<PlayerHistoryMetric> buildMetrics(PlayerHistoryEntry entry,
                                                   PlayerHistoryMetricsDto metrics,
                                                   PositionGroup group) {
        List<PlayerHistoryMetric> result = new ArrayList<>();
        if (group == PositionGroup.GOALKEEPER && metrics.goalkeeper() != null) {
            var value = metrics.goalkeeper();
            add(result, entry, PlayerMetricCode.SAVES, value.saves());
            add(result, entry, PlayerMetricCode.GOALS_CONCEDED, value.goalsConceded());
            addBoolean(result, entry, PlayerMetricCode.CLEAN_SHEET, value.cleanSheet());
            add(result, entry, PlayerMetricCode.CROSS_INTERVENTIONS, value.crossInterventions());
            add(result, entry, PlayerMetricCode.GK_PASSES_ATTEMPTED, value.passesAttempted());
            add(result, entry, PlayerMetricCode.GK_PASSES_COMPLETED, value.passesCompleted());
        }
        if (group == PositionGroup.DEFENDER && metrics.defender() != null) {
            var value = metrics.defender();
            add(result, entry, PlayerMetricCode.SUCCESSFUL_TACKLES, value.successfulTackles());
            add(result, entry, PlayerMetricCode.DEF_INTERCEPTIONS, value.interceptions());
            add(result, entry, PlayerMetricCode.CLEARANCES, value.clearances());
            add(result, entry, PlayerMetricCode.DEF_DUELS_WON, value.duelsWon());
            add(result, entry, PlayerMetricCode.BLOCKS, value.blocks());
            add(result, entry, PlayerMetricCode.ERRORS_LEADING_TO_CHANCE, value.errorsLeadingToChance());
        }
        if (group == PositionGroup.MIDFIELDER && metrics.midfielder() != null) {
            var value = metrics.midfielder();
            add(result, entry, PlayerMetricCode.MID_PASSES_ATTEMPTED, value.passesAttempted());
            add(result, entry, PlayerMetricCode.MID_PASSES_COMPLETED, value.passesCompleted());
            add(result, entry, PlayerMetricCode.MID_INTERCEPTIONS, value.interceptions());
            add(result, entry, PlayerMetricCode.KEY_PASSES, value.keyPasses());
            add(result, entry, PlayerMetricCode.MID_ASSISTS, value.assists());
            add(result, entry, PlayerMetricCode.RECOVERIES, value.recoveries());
            add(result, entry, PlayerMetricCode.MID_DUELS_WON, value.duelsWon());
            add(result, entry, PlayerMetricCode.MID_SUCCESSFUL_DRIBBLES, value.successfulDribbles());
            add(result, entry, PlayerMetricCode.MID_SHOTS, value.shots());
        }
        if (group == PositionGroup.FORWARD && metrics.forward() != null) {
            var value = metrics.forward();
            add(result, entry, PlayerMetricCode.FWD_GOALS, value.goals());
            add(result, entry, PlayerMetricCode.FWD_ASSISTS, value.assists());
            add(result, entry, PlayerMetricCode.FWD_SHOTS, value.shots());
            add(result, entry, PlayerMetricCode.FWD_SUCCESSFUL_DRIBBLES, value.successfulDribbles());
            add(result, entry, PlayerMetricCode.CHANCES_CREATED, value.chancesCreated());
            add(result, entry, PlayerMetricCode.CROSSES_ATTEMPTED, value.crossesAttempted());
            add(result, entry, PlayerMetricCode.CROSSES_COMPLETED, value.crossesCompleted());
        }
        return result;
    }

    private void add(List<PlayerHistoryMetric> target, PlayerHistoryEntry entry,
                     PlayerMetricCode code, Integer value) {
        if (value == null) {
            return;
        }
        PlayerHistoryMetric metric = new PlayerHistoryMetric();
        metric.setHistoryEntry(entry);
        metric.setMetricGroup(code.group());
        metric.setMetricCode(code.code());
        metric.setNumericValue(BigDecimal.valueOf(value));
        target.add(metric);
    }

    private void addBoolean(List<PlayerHistoryMetric> target, PlayerHistoryEntry entry,
                            PlayerMetricCode code, Boolean value) {
        if (value == null) {
            return;
        }
        add(target, entry, code, value ? 1 : 0);
    }

    private void saveRevision(PlayerHistoryEntry entry, HistoryChangeType type, String reason,
                              PlayerHistoryEntryResponse response) {
        PlayerHistoryRevision revision = new PlayerHistoryRevision();
        revision.setHistoryEntry(entry);
        revision.setRevisionNumber(entry.getRevisionNumber());
        revision.setChangeType(type);
        revision.setReason(reason);
        revision.setSnapshotJson(writeSnapshot(response));
        revision.setEditedBy(entry.getLastModifiedBy());
        revision.setEditedAt(Instant.now());
        revisionRepository.saveAndFlush(revision);
    }

    private String writeSnapshot(PlayerHistoryEntryResponse response) {
        return objectMapper.writeValueAsString(response);
    }

    private JsonNode readSnapshot(String json) {
        return objectMapper.readTree(json);
    }

    private TeamResult expectedResult(int teamGoals, int opponentGoals) {
        if (teamGoals > opponentGoals) {
            return TeamResult.WIN;
        }
        if (teamGoals < opponentGoals) {
            return TeamResult.LOSS;
        }
        return TeamResult.DRAW;
    }

    private Pageable pageable(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String requested = sort == null || sort.isBlank() ? "activityDate,desc" : sort.trim();
        String[] parts = requested.split(",", -1);
        if (parts.length < 1 || parts.length > 2) {
            throw ApiException.badRequest("INVALID_SORT", "El orden solicitado no es valido.");
        }
        String property = parts[0].trim();
        if (!SORT_PROPERTIES.contains(property)) {
            throw ApiException.badRequest("INVALID_SORT", "El orden solicitado no es valido.");
        }
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length == 2) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("INVALID_SORT", "El orden solicitado no es valido.");
            }
        }
        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }

    private void validateDates(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw ApiException.badRequest("INVALID_FILTER_COMBINATION",
                    "La fecha inicial no puede ser posterior a la final.");
        }
    }

    private boolean nonZero(Integer value) {
        return value != null && value != 0;
    }

    private boolean hasAnyMetrics(PlayerHistoryMetricsDto metrics) {
        return metrics != null && (metrics.goalkeeper() != null || metrics.defender() != null
                || metrics.midfielder() != null || metrics.forward() != null);
    }

    private PlayerProfile lockProfile(Long playerId) {
        return playerProfileRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> ApiException.notFound("PLAYER_NOT_FOUND", "Jugador no encontrado."));
    }

    private User operator(AppUserPrincipal actor) {
        return userRepository.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Credenciales invalidas."));
    }

    private ApiException notFound() {
        return ApiException.notFound("PLAYER_HISTORY_NOT_FOUND", "Entrada de historial no encontrada.");
    }

    private record HistoryData(
            ActivityType activityType,
            LocalDate activityDate,
            String competition,
            String opponent,
            AgeCategory ageCategory,
            CompetitiveLevel competitiveLevel,
            PlayerPosition positionPlayed,
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

        static HistoryData from(PlayerHistoryEntryCreateRequest request) {
            return new HistoryData(
                    request.activityType(), request.activityDate(), optional(request.competition()),
                    optional(request.opponent()), request.ageCategory(), request.competitiveLevel(),
                    request.positionPlayed(), request.participation(), request.minutesPlayed(),
                    request.teamResult(), request.teamGoals(), request.opponentGoals(),
                    request.playerGoals(), request.assists(), request.yellowCards(), request.redCards(),
                    optional(request.observations()), optional(request.videoUrl()),
                    request.videoTimestampSeconds(), optional(request.videoDescription()),
                    request.metrics(), request.ratings(), optional(request.strengths()),
                    optional(request.improvements()), optional(request.recommendations()));
        }

        static HistoryData from(PlayerHistoryEntryCorrectionRequest request) {
            return new HistoryData(
                    request.activityType(), request.activityDate(), optional(request.competition()),
                    optional(request.opponent()), request.ageCategory(), request.competitiveLevel(),
                    request.positionPlayed(), request.participation(), request.minutesPlayed(),
                    request.teamResult(), request.teamGoals(), request.opponentGoals(),
                    request.playerGoals(), request.assists(), request.yellowCards(), request.redCards(),
                    optional(request.observations()), optional(request.videoUrl()),
                    request.videoTimestampSeconds(), optional(request.videoDescription()),
                    request.metrics(), request.ratings(), optional(request.strengths()),
                    optional(request.improvements()), optional(request.recommendations()));
        }

        private static String optional(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim();
            return normalized.isEmpty() ? null : normalized;
        }
    }
}
