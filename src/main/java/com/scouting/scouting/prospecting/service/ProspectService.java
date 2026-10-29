package com.scouting.scouting.prospecting.service;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.entity.PlayerProfileStatus;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.player.service.PlayerTime;
import com.scouting.scouting.prospecting.dto.ProspectCreateRequest;
import com.scouting.scouting.prospecting.dto.ProspectEvaluationCreateRequest;
import com.scouting.scouting.prospecting.dto.ProspectEvaluationResponse;
import com.scouting.scouting.prospecting.dto.ProspectNotesUpdateRequest;
import com.scouting.scouting.prospecting.dto.ProspectResponse;
import com.scouting.scouting.prospecting.dto.ProspectStatusUpdateRequest;
import com.scouting.scouting.prospecting.entity.Prospect;
import com.scouting.scouting.prospecting.entity.ProspectEvaluation;
import com.scouting.scouting.prospecting.entity.ProspectStatus;
import com.scouting.scouting.prospecting.repository.ProspectEvaluationRepository;
import com.scouting.scouting.prospecting.repository.ProspectRepository;
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

import java.time.Instant;
import java.time.LocalDate;

/** Servicio privado de prospecciones y evaluaciones de ojeador. */
@Service
public class ProspectService {

    private final ProspectRepository prospectRepository;
    private final ProspectEvaluationRepository evaluationRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserRepository userRepository;
    private final ProspectMapper mapper;

    public ProspectService(ProspectRepository prospectRepository,
                           ProspectEvaluationRepository evaluationRepository,
                           PlayerProfileRepository playerProfileRepository,
                           UserOrganizationRepository userOrganizationRepository,
                           UserRepository userRepository,
                           ProspectMapper mapper) {
        this.prospectRepository = prospectRepository;
        this.evaluationRepository = evaluationRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.userOrganizationRepository = userOrganizationRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProspectResponse> list(AppUserPrincipal actor,
                                               int page,
                                               int size,
                                               String sort,
                                               ProspectStatus status,
                                               Long playerId,
                                               String q) {
        requireScout(actor);
        Pageable pageable = ProspectPageRequestFactory.create(page, size, sort);
        Page<Prospect> result = prospectRepository.searchOwn(
                actor.id(), status, playerId, ProspectSearchQuery.containsPattern(q), pageable);
        return PageResponse.from(result.map(prospect ->
                mapper.toResponse(prospect, organization(prospect.getPlayer()))));
    }

    @Transactional
    public ProspectResponse create(AppUserPrincipal actor, ProspectCreateRequest request) {
        requireScout(actor);
        PlayerProfile player = playerProfileRepository.findDetailById(request.playerId())
                .orElseThrow(this::playerNotFound);
        UserOrganization organization = organization(player);
        if (!isEligible(player, organization)) {
            throw playerNotFound();
        }
        if (prospectRepository.existsByScout_IdAndPlayer_Id(actor.id(), player.getId())) {
            throw prospectAlreadyExists();
        }

        Instant now = Instant.now();
        Prospect prospect = new Prospect();
        prospect.setScout(operator(actor));
        prospect.setPlayer(player);
        prospect.setStatus(ProspectStatus.FOLLOWING);
        prospect.setPrivateNotes(optional(request.privateNotes()));
        prospect.setStartedAt(now);
        prospect.setLastActivityAt(now);
        prospect.setClosedAt(null);
        try {
            Prospect saved = prospectRepository.saveAndFlush(prospect);
            return mapper.toResponse(saved, organization);
        } catch (DataIntegrityViolationException ex) {
            if (ProspectDataIntegrity.isScoutPlayerUniqueViolation(ex)) {
                throw prospectAlreadyExists();
            }
            throw ApiException.conflict("DATA_CONFLICT", "No se pudo completar la operacion.");
        }
    }

    @Transactional(readOnly = true)
    public ProspectResponse get(AppUserPrincipal actor, Long id) {
        requireScout(actor);
        Prospect prospect = findOwned(actor, id);
        return mapper.toResponse(prospect, organization(prospect.getPlayer()));
    }

    @Transactional
    public ProspectResponse updateNotes(AppUserPrincipal actor, Long id, ProspectNotesUpdateRequest request) {
        requireScout(actor);
        Prospect prospect = findOwnedForUpdate(actor, id);
        requireVersion(prospect, request.version());
        prospect.setPrivateNotes(optional(request.privateNotes()));
        prospect.setLastActivityAt(Instant.now());
        return mapper.toResponse(prospectRepository.saveAndFlush(prospect), organization(prospect.getPlayer()));
    }

    @Transactional
    public ProspectResponse updateStatus(AppUserPrincipal actor, Long id, ProspectStatusUpdateRequest request) {
        requireScout(actor);
        Prospect prospect = findOwnedForUpdate(actor, id);
        requireVersion(prospect, request.version());
        if (!prospect.getStatus().canTransitionTo(request.status())) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION",
                    "La transicion de estado solicitada no es valida.");
        }

        Instant now = Instant.now();
        prospect.setStatus(request.status());
        prospect.setLastActivityAt(now);
        prospect.setClosedAt(request.status() == ProspectStatus.CLOSED ? now : null);
        return mapper.toResponse(prospectRepository.saveAndFlush(prospect), organization(prospect.getPlayer()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProspectEvaluationResponse> listEvaluations(AppUserPrincipal actor, Long prospectId,
                                                                    int page, int size) {
        requireScout(actor);
        findOwned(actor, prospectId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "evaluatedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<ProspectEvaluation> result = evaluationRepository.findByProspect_Id(prospectId, pageable);
        return PageResponse.from(result.map(mapper::toEvaluation));
    }

    @Transactional
    public ProspectEvaluationResponse createEvaluation(AppUserPrincipal actor, Long prospectId,
                                                       ProspectEvaluationCreateRequest request) {
        requireScout(actor);
        Prospect prospect = findOwnedForUpdate(actor, prospectId);
        if (prospect.getStatus() == ProspectStatus.CLOSED) {
            throw ApiException.conflict("PROSPECT_CLOSED",
                    "No se pueden agregar evaluaciones a una prospeccion cerrada.");
        }
        PlayerProfile player = prospect.getPlayer();
        UserOrganization organization = organization(player);
        if (!isEligible(player, organization)) {
            throw ApiException.conflict("PLAYER_NOT_ELIGIBLE",
                    "El jugador ya no es elegible para nuevas evaluaciones.");
        }
        validateEvaluationDate(player, request.evaluatedAt());

        ProspectEvaluation evaluation = new ProspectEvaluation();
        evaluation.setProspect(prospect);
        evaluation.setCreatedBy(operator(actor));
        evaluation.setEvaluatedAt(request.evaluatedAt());
        evaluation.setTechnicalRating(request.technicalRating());
        evaluation.setTacticalRating(request.tacticalRating());
        evaluation.setPhysicalRating(request.physicalRating());
        evaluation.setMentalRating(request.mentalRating());
        evaluation.setOverallRating(request.overallRating());
        evaluation.setStrengths(optional(request.strengths()));
        evaluation.setImprovements(optional(request.improvements()));
        evaluation.setRecommendation(optional(request.recommendation()));
        evaluation.setPrivateNotes(optional(request.privateNotes()));
        evaluation.setVideoUrl(optional(request.videoUrl()));

        ProspectEvaluation saved = evaluationRepository.saveAndFlush(evaluation);
        prospect.setLastActivityAt(Instant.now());
        prospectRepository.saveAndFlush(prospect);
        return mapper.toEvaluation(saved);
    }

    @Transactional(readOnly = true)
    public ProspectEvaluationResponse getEvaluation(AppUserPrincipal actor, Long prospectId, Long evaluationId) {
        requireScout(actor);
        findOwned(actor, prospectId);
        ProspectEvaluation evaluation = evaluationRepository.findByIdAndProspect_Id(evaluationId, prospectId)
                .orElseThrow(this::evaluationNotFound);
        return mapper.toEvaluation(evaluation);
    }

    private void validateEvaluationDate(PlayerProfile player, LocalDate evaluatedAt) {
        if (evaluatedAt == null) {
            throw ApiException.badRequest("VALIDATION_ERROR", "La fecha de evaluacion es obligatoria.");
        }
        if (evaluatedAt.isAfter(PlayerTime.today())) {
            throw ApiException.badRequest("INVALID_EVALUATION_DATE",
                    "La fecha de evaluacion no puede ser futura.");
        }
        if (evaluatedAt.isBefore(player.getBirthDate())) {
            throw ApiException.badRequest("INVALID_EVALUATION_DATE",
                    "La fecha de evaluacion no puede ser anterior al nacimiento del jugador.");
        }
    }

    private Prospect findOwned(AppUserPrincipal actor, Long id) {
        return prospectRepository.findByScout_IdAndId(actor.id(), id)
                .orElseThrow(this::prospectNotFound);
    }

    private Prospect findOwnedForUpdate(AppUserPrincipal actor, Long id) {
        return prospectRepository.findByScout_IdAndIdForUpdate(actor.id(), id)
                .orElseThrow(this::prospectNotFound);
    }

    private boolean isEligible(PlayerProfile player, UserOrganization organization) {
        if (player.getOperationalStatus() != PlayerOperationalStatus.ACTIVE
                || player.getProfileStatus() != PlayerProfileStatus.COMPLETE
                || player.getUser().getStatus() != UserStatus.ACTIVE) {
            return false;
        }
        return organization != null
                && organization.getAcademy() != null
                && organization.getAcademy().getOperationalStatus() == OperationalStatus.ACTIVE;
    }

    private UserOrganization organization(PlayerProfile player) {
        return userOrganizationRepository.findByUser_Id(player.getUser().getId()).orElse(null);
    }

    private void requireVersion(Prospect prospect, Long version) {
        if (!prospect.getVersion().equals(version)) {
            throw ApiException.conflict("STALE_PROSPECT", "La prospeccion fue modificada por otra operacion.");
        }
    }

    private User operator(AppUserPrincipal actor) {
        return userRepository.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Credenciales invalidas."));
    }

    private void requireScout(AppUserPrincipal actor) {
        if (actor == null) {
            throw ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Credenciales invalidas.");
        }
        if (actor.role() != Role.SCOUT) {
            throw ApiException.forbidden("ACCESS_DENIED", "No tiene permisos para realizar esta accion.");
        }
    }

    private String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ApiException playerNotFound() {
        return ApiException.notFound("PLAYER_NOT_FOUND", "Jugador no encontrado.");
    }

    private ApiException prospectNotFound() {
        return ApiException.notFound("PROSPECT_NOT_FOUND", "Prospeccion no encontrada.");
    }

    private ApiException evaluationNotFound() {
        return ApiException.notFound("PROSPECT_EVALUATION_NOT_FOUND", "Evaluacion no encontrada.");
    }

    private ApiException prospectAlreadyExists() {
        return ApiException.conflict("PROSPECT_ALREADY_EXISTS", "Ya sigue a este jugador.");
    }
}
