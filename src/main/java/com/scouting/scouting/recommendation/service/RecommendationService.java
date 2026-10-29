package com.scouting.scouting.recommendation.service;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.entity.VerificationStatus;
import com.scouting.scouting.organization.repository.ClubRepository;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.organization.service.OrganizationScopeService;
import com.scouting.scouting.player.dto.PlayerHistoryEntryResponse;
import com.scouting.scouting.player.entity.GuardianConsentStatus;
import com.scouting.scouting.player.entity.PlayerGuardianConsentEvent;
import com.scouting.scouting.player.entity.PlayerHistoryEntry;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.entity.PlayerProfileStatus;
import com.scouting.scouting.player.repository.PlayerGuardianConsentEventRepository;
import com.scouting.scouting.player.repository.PlayerHistoryEntryRepository;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.player.service.PlayerMapper;
import com.scouting.scouting.player.service.PlayerTime;
import com.scouting.scouting.prospecting.entity.Prospect;
import com.scouting.scouting.prospecting.entity.ProspectEvaluation;
import com.scouting.scouting.prospecting.repository.ProspectEvaluationRepository;
import com.scouting.scouting.prospecting.repository.ProspectRepository;
import com.scouting.scouting.recommendation.dto.RecommendationCreateRequest;
import com.scouting.scouting.recommendation.dto.RecommendationDetailResponse;
import com.scouting.scouting.recommendation.dto.RecommendationSnapshot;
import com.scouting.scouting.recommendation.dto.RecommendationSummaryResponse;
import com.scouting.scouting.recommendation.entity.Recommendation;
import com.scouting.scouting.recommendation.entity.RecommendationAuthorizationType;
import com.scouting.scouting.recommendation.entity.RecommendationEvaluation;
import com.scouting.scouting.recommendation.entity.RecommendationHistoryEntry;
import com.scouting.scouting.recommendation.entity.RecommendationResponse;
import com.scouting.scouting.recommendation.entity.RecommendationStatus;
import com.scouting.scouting.recommendation.dto.RecommendationClubReplyRequest;
import com.scouting.scouting.recommendation.repository.RecommendationEvaluationRepository;
import com.scouting.scouting.recommendation.repository.RecommendationHistoryEntryRepository;
import com.scouting.scouting.recommendation.repository.RecommendationRepository;
import com.scouting.scouting.recommendation.repository.RecommendationResponseRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Recomendaciones a clubes y respuestas append-only. */
@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationResponseRepository responseRepository;
    private final RecommendationHistoryEntryRepository historySelectionRepository;
    private final RecommendationEvaluationRepository evaluationSelectionRepository;
    private final ProspectRepository prospectRepository;
    private final ProspectEvaluationRepository prospectEvaluationRepository;
    private final PlayerHistoryEntryRepository playerHistoryEntryRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerGuardianConsentEventRepository consentRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final OrganizationScopeService scopeService;
    private final PlayerMapper playerMapper;
    private final RecommendationMapper mapper;
    private final ObjectMapper objectMapper;

    public RecommendationService(RecommendationRepository recommendationRepository,
                                 RecommendationResponseRepository responseRepository,
                                 RecommendationHistoryEntryRepository historySelectionRepository,
                                 RecommendationEvaluationRepository evaluationSelectionRepository,
                                 ProspectRepository prospectRepository,
                                 ProspectEvaluationRepository prospectEvaluationRepository,
                                 PlayerHistoryEntryRepository playerHistoryEntryRepository,
                                 PlayerProfileRepository playerProfileRepository,
                                 PlayerGuardianConsentEventRepository consentRepository,
                                 UserOrganizationRepository userOrganizationRepository,
                                 ClubRepository clubRepository,
                                 UserRepository userRepository,
                                 OrganizationScopeService scopeService,
                                 PlayerMapper playerMapper,
                                 RecommendationMapper mapper,
                                 ObjectMapper objectMapper) {
        this.recommendationRepository = recommendationRepository;
        this.responseRepository = responseRepository;
        this.historySelectionRepository = historySelectionRepository;
        this.evaluationSelectionRepository = evaluationSelectionRepository;
        this.prospectRepository = prospectRepository;
        this.prospectEvaluationRepository = prospectEvaluationRepository;
        this.playerHistoryEntryRepository = playerHistoryEntryRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.consentRepository = consentRepository;
        this.userOrganizationRepository = userOrganizationRepository;
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
        this.scopeService = scopeService;
        this.playerMapper = playerMapper;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<RecommendationSummaryResponse> listForProspect(AppUserPrincipal actor,
                                                                        Long prospectId,
                                                                        int page,
                                                                        int size) {
        requireScout(actor);
        findOwnedProspect(actor, prospectId);
        Page<Recommendation> result = recommendationRepository.findByProspect_IdAndScout_Id(
                prospectId, actor.id(), pageable(page, size));
        return PageResponse.from(result.map(recommendation ->
                mapper.summary(recommendation, latestResponse(recommendation.getId()))));
    }

    @Transactional
    public RecommendationDetailResponse create(AppUserPrincipal actor, Long prospectId,
                                               RecommendationCreateRequest request) {
        requireScout(actor);
        Prospect prospect = findOwnedProspectForUpdate(actor, prospectId);
        // Bloquea el perfil para serializar el envio con la revocacion del consentimiento.
        PlayerProfile player = playerProfileRepository.findByIdForUpdate(prospect.getPlayer().getId())
                .orElseThrow(() -> ApiException.conflict("PLAYER_NOT_ELIGIBLE",
                        "El jugador ya no es elegible para una recomendacion."));
        UserOrganization organization = organization(player);
        if (!isPlayerEligible(player, organization)) {
            throw ApiException.conflict("PLAYER_NOT_ELIGIBLE",
                    "El jugador ya no es elegible para una recomendacion.");
        }
        Club club = clubRepository.findByIdForUpdate(request.clubId())
                .orElseThrow(() -> ApiException.notFound("CLUB_NOT_FOUND", "Club no encontrado."));
        if (club.getOperationalStatus() != OperationalStatus.ACTIVE
                || club.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw ApiException.conflict("CLUB_NOT_ELIGIBLE",
                    "El club debe estar activo y verificado.");
        }
        if (recommendationRepository.existsByProspect_IdAndClub_Id(prospect.getId(), club.getId())) {
            throw recommendationAlreadyExists();
        }

        List<Long> historyIds = normalizeIds(request.historyEntryIds());
        List<Long> evaluationIds = normalizeIds(request.evaluationIds());
        List<PlayerHistoryEntry> historyEntries = selectedHistory(historyIds, player.getId());
        List<ProspectEvaluation> evaluations = selectedEvaluations(evaluationIds, prospect.getId());
        RecommendationSnapshot snapshot = snapshot(player, organization, historyEntries, evaluations);
        String snapshotJson = objectMapper.writeValueAsString(snapshot);

        Instant now = Instant.now();
        RecommendationAuthorization authorization = authorization(prospect, player, now);
        Recommendation recommendation = new Recommendation();
        recommendation.setProspect(prospect);
        recommendation.setClub(club);
        recommendation.setScout(prospect.getScout());
        recommendation.setStatus(RecommendationStatus.SENT);
        recommendation.setMessage(request.message().trim());
        recommendation.setSnapshotJson(snapshotJson);
        recommendation.setAuthorizationType(authorization.type());
        recommendation.setAuthorizedAt(authorization.authorizedAt());
        recommendation.setSentAt(now);
        try {
            Recommendation saved = recommendationRepository.saveAndFlush(recommendation);
            saveSelections(saved, historyEntries, evaluations);
            return mapper.detail(saved, List.of());
        } catch (DataIntegrityViolationException ex) {
            if (RecommendationDataIntegrity.isProspectClubUniqueViolation(ex)) {
                throw recommendationAlreadyExists();
            }
            throw ApiException.conflict("DATA_CONFLICT", "No se pudo completar la operacion.");
        }
    }

    @Transactional(readOnly = true)
    public RecommendationDetailResponse getForScout(AppUserPrincipal actor, Long id) {
        requireScout(actor);
        Recommendation recommendation = recommendationRepository.findByIdAndScout_Id(id, actor.id())
                .orElseThrow(this::recommendationNotFound);
        return mapper.detail(recommendation, responses(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<RecommendationSummaryResponse> listForClub(AppUserPrincipal actor, int page, int size) {
        Club club = requireClub(actor);
        Page<Recommendation> result = recommendationRepository.findByClub_Id(club.getId(), pageable(page, size));
        return PageResponse.from(result.map(recommendation ->
                mapper.summary(recommendation, latestResponse(recommendation.getId()))));
    }

    @Transactional(readOnly = true)
    public RecommendationDetailResponse getForClub(AppUserPrincipal actor, Long id) {
        Club club = requireClub(actor);
        Recommendation recommendation = recommendationRepository.findByIdAndClub_Id(id, club.getId())
                .orElseThrow(this::recommendationNotFound);
        return mapper.detail(recommendation, responses(id));
    }

    @Transactional
    public RecommendationDetailResponse respond(AppUserPrincipal actor, Long id,
                                                RecommendationClubReplyRequest request) {
        Club club = requireClub(actor);
        Recommendation recommendation = recommendationRepository
                .findByIdAndClub_IdForUpdate(id, club.getId())
                .orElseThrow(this::recommendationNotFound);
        if (club.getOperationalStatus() != OperationalStatus.ACTIVE) {
            throw ApiException.conflict("CLUB_INACTIVE",
                    "El club esta inhabilitado y no puede responder recomendaciones.");
        }

        RecommendationResponse response = new RecommendationResponse();
        response.setRecommendation(recommendation);
        response.setClub(club);
        response.setRespondedBy(operator(actor));
        response.setStatus(request.status());
        response.setMessage(request.message().trim());
        response.setRespondedAt(Instant.now());
        responseRepository.saveAndFlush(response);

        recommendation.setStatus(RecommendationStatus.RESPONDED);
        recommendationRepository.saveAndFlush(recommendation);
        return mapper.detail(recommendation, responses(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<com.scouting.scouting.recommendation.dto.RecommendationAdminResponse> listAdmin(
            AppUserPrincipal actor, int page, int size) {
        requireAdmin(actor);
        Page<Recommendation> result = recommendationRepository.findAll(pageable(page, size));
        return PageResponse.from(result.map(recommendation ->
                mapper.admin(recommendation, latestResponse(recommendation.getId()))));
    }

    @Transactional(readOnly = true)
    public com.scouting.scouting.recommendation.dto.RecommendationAdminResponse getAdmin(
            AppUserPrincipal actor, Long id) {
        requireAdmin(actor);
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(this::recommendationNotFound);
        return mapper.admin(recommendation, latestResponse(id));
    }

    private void saveSelections(Recommendation recommendation,
                                List<PlayerHistoryEntry> histories,
                                List<ProspectEvaluation> evaluations) {
        if (!histories.isEmpty()) {
            List<RecommendationHistoryEntry> links = new ArrayList<>();
            for (PlayerHistoryEntry history : histories) {
                RecommendationHistoryEntry link = new RecommendationHistoryEntry();
                link.setRecommendation(recommendation);
                link.setHistoryEntry(history);
                links.add(link);
            }
            historySelectionRepository.saveAllAndFlush(links);
        }
        if (!evaluations.isEmpty()) {
            List<RecommendationEvaluation> links = new ArrayList<>();
            for (ProspectEvaluation evaluation : evaluations) {
                RecommendationEvaluation link = new RecommendationEvaluation();
                link.setRecommendation(recommendation);
                link.setEvaluation(evaluation);
                links.add(link);
            }
            evaluationSelectionRepository.saveAllAndFlush(links);
        }
    }

    private RecommendationSnapshot snapshot(PlayerProfile player,
                                            UserOrganization organization,
                                            List<PlayerHistoryEntry> histories,
                                            List<ProspectEvaluation> evaluations) {
        RecommendationSnapshot.PlayerSnapshot playerSnapshot = new RecommendationSnapshot.PlayerSnapshot(
                player.getUser().getFirstName(),
                player.getUser().getLastName(),
                PlayerTime.age(player.getBirthDate(), PlayerTime.today()),
                player.getPosition(),
                player.getPosition() == null ? null : player.getPosition().group(),
                player.getBestPosition(),
                player.getBestPosition() == null ? null : player.getBestPosition().group(),
                player.getDominantFoot(),
                player.getHeightCm(),
                player.getWeightKg(),
                player.getSummary(),
                player.getPhotoUrl(),
                playerMapper.academy(organization));

        List<RecommendationSnapshot.HistorySnapshot> historySnapshots = histories.stream()
                .map(history -> historySnapshot(playerMapper.toHistory(history)))
                .toList();
        List<RecommendationSnapshot.EvaluationSnapshot> evaluationSnapshots = evaluations.stream()
                .map(evaluation -> new RecommendationSnapshot.EvaluationSnapshot(
                        evaluation.getId(),
                        evaluation.getEvaluatedAt(),
                        evaluation.getTechnicalRating(),
                        evaluation.getTacticalRating(),
                        evaluation.getPhysicalRating(),
                        evaluation.getMentalRating(),
                        evaluation.getOverallRating(),
                        evaluation.getStrengths(),
                        evaluation.getImprovements(),
                        evaluation.getRecommendation(),
                        evaluation.getVideoUrl()))
                .toList();

        return new RecommendationSnapshot(
                playerSnapshot,
                playerMapper.residence(player.getResidenceUbigeoCode(), false),
                historySnapshots,
                evaluationSnapshots);
    }

    private RecommendationSnapshot.HistorySnapshot historySnapshot(PlayerHistoryEntryResponse history) {
        return new RecommendationSnapshot.HistorySnapshot(
                history.id(),
                history.activityType(),
                history.activityDate(),
                history.competition(),
                history.opponent(),
                history.ageCategory(),
                history.competitiveLevel(),
                history.positionPlayed(),
                history.positionGroup(),
                history.participation(),
                history.minutesPlayed(),
                history.teamResult(),
                history.teamGoals(),
                history.opponentGoals(),
                history.playerGoals(),
                history.assists(),
                history.yellowCards(),
                history.redCards(),
                history.observations(),
                history.videoUrl(),
                history.videoTimestampSeconds(),
                history.videoDescription(),
                history.metrics(),
                history.ratings(),
                history.strengths(),
                history.improvements(),
                history.recommendations());
    }

    private RecommendationAuthorization authorization(Prospect prospect, PlayerProfile player, Instant now) {
        if (PlayerTime.isMinor(player.getBirthDate(), PlayerTime.today())) {
            PlayerGuardianConsentEvent consent = consentRepository
                    .findTopByPlayerProfile_IdOrderByRecordedAtDescIdDesc(player.getId())
                    .orElse(null);
            if (consent == null || consent.getStatus() != GuardianConsentStatus.GRANTED) {
                throw ApiException.conflict("GUARDIAN_CONSENT_REQUIRED",
                        "Se requiere consentimiento vigente del tutor para recomendar a un menor.");
            }
            return new RecommendationAuthorization(
                    RecommendationAuthorizationType.GUARDIAN_CONSENT, consent.getRecordedAt());
        }
        return new RecommendationAuthorization(RecommendationAuthorizationType.SCOUT_SEND, now);
    }

    private List<PlayerHistoryEntry> selectedHistory(List<Long> ids, Long playerId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<PlayerHistoryEntry> entries = playerHistoryEntryRepository
                .findByIdInAndPlayerProfile_Id(ids, playerId);
        if (entries.size() != ids.size()) {
            throw invalidSelection();
        }
        Map<Long, PlayerHistoryEntry> byId = entries.stream()
                .collect(Collectors.toMap(PlayerHistoryEntry::getId, Function.identity()));
        return ids.stream().map(byId::get).toList();
    }

    private List<ProspectEvaluation> selectedEvaluations(List<Long> ids, Long prospectId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<ProspectEvaluation> evaluations = prospectEvaluationRepository
                .findByIdInAndProspect_Id(ids, prospectId);
        if (evaluations.size() != ids.size()) {
            throw invalidSelection();
        }
        Map<Long, ProspectEvaluation> byId = evaluations.stream()
                .collect(Collectors.toMap(ProspectEvaluation::getId, Function.identity()));
        return ids.stream().map(byId::get).toList();
    }

    private List<Long> normalizeIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private Prospect findOwnedProspect(AppUserPrincipal actor, Long prospectId) {
        return prospectRepository.findByScout_IdAndId(actor.id(), prospectId)
                .orElseThrow(() -> ApiException.notFound("PROSPECT_NOT_FOUND", "Prospeccion no encontrada."));
    }

    private Prospect findOwnedProspectForUpdate(AppUserPrincipal actor, Long prospectId) {
        return prospectRepository.findByScout_IdAndIdForUpdate(actor.id(), prospectId)
                .orElseThrow(() -> ApiException.notFound("PROSPECT_NOT_FOUND", "Prospeccion no encontrada."));
    }

    private UserOrganization organization(PlayerProfile player) {
        return userOrganizationRepository.findByUser_Id(player.getUser().getId()).orElse(null);
    }

    private boolean isPlayerEligible(PlayerProfile player, UserOrganization organization) {
        if (player.getOperationalStatus() != PlayerOperationalStatus.ACTIVE
                || player.getProfileStatus() != PlayerProfileStatus.COMPLETE
                || player.getUser().getStatus() != UserStatus.ACTIVE) {
            return false;
        }
        Academy academy = organization == null ? null : organization.getAcademy();
        return academy != null && academy.getOperationalStatus() == OperationalStatus.ACTIVE;
    }

    private Club requireClub(AppUserPrincipal actor) {
        scopeService.requireRole(actor, Set.of(Role.CLUB_MANAGER));
        return scopeService.requireClub(actor, Set.of(Role.CLUB_MANAGER));
    }

    private void requireScout(AppUserPrincipal actor) {
        if (actor == null) {
            throw ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Credenciales invalidas.");
        }
        if (actor.role() != Role.SCOUT) {
            throw ApiException.forbidden("ACCESS_DENIED", "No tiene permisos para realizar esta accion.");
        }
    }

    private void requireAdmin(AppUserPrincipal actor) {
        if (actor == null) {
            throw ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Credenciales invalidas.");
        }
        if (actor.role() != Role.ADMIN) {
            throw ApiException.forbidden("ACCESS_DENIED", "No tiene permisos para realizar esta accion.");
        }
    }

    private User operator(AppUserPrincipal actor) {
        return userRepository.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Credenciales invalidas."));
    }

    private List<RecommendationResponse> responses(Long recommendationId) {
        return responseRepository.findByRecommendation_IdOrderByRespondedAtDescIdDesc(recommendationId);
    }

    private RecommendationResponse latestResponse(Long recommendationId) {
        return responseRepository.findTopByRecommendation_IdOrderByRespondedAtDescIdDesc(recommendationId)
                .orElse(null);
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "sentAt"));
    }

    private ApiException recommendationNotFound() {
        return ApiException.notFound("RECOMMENDATION_NOT_FOUND", "Recomendacion no encontrada.");
    }

    private ApiException recommendationAlreadyExists() {
        return ApiException.conflict("RECOMMENDATION_ALREADY_EXISTS",
                "Ya existe una recomendacion para este jugador y club.");
    }

    private ApiException invalidSelection() {
        return ApiException.badRequest("INVALID_SELECTION",
                "La seleccion contiene elementos inexistentes o de otro jugador/prospeccion.");
    }

    private record RecommendationAuthorization(RecommendationAuthorizationType type, Instant authorizedAt) {
    }
}
