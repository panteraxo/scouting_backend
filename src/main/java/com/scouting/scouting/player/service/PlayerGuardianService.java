package com.scouting.scouting.player.service;

import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.player.dto.GuardianConsentUpdateRequest;
import com.scouting.scouting.player.dto.PlayerGuardianScoutResponse;
import com.scouting.scouting.player.dto.PlayerGuardianStaffResponse;
import com.scouting.scouting.player.dto.PlayerGuardianUpsertRequest;
import com.scouting.scouting.player.entity.GuardianConsentStatus;
import com.scouting.scouting.player.entity.PlayerGuardian;
import com.scouting.scouting.player.entity.PlayerGuardianConsentEvent;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.repository.PlayerGuardianConsentEventRepository;
import com.scouting.scouting.player.repository.PlayerGuardianRepository;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/** Datos restringidos del tutor y consentimiento. */
@Service
public class PlayerGuardianService {

    private final PlayerGuardianRepository guardianRepository;
    private final PlayerGuardianConsentEventRepository consentRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final UserRepository userRepository;
    private final PlayerAccessService accessService;
    private final PlayerMapper mapper;
    private final PlayerProfileService profileService;

    public PlayerGuardianService(PlayerGuardianRepository guardianRepository,
                                 PlayerGuardianConsentEventRepository consentRepository,
                                 PlayerProfileRepository playerProfileRepository,
                                 UserRepository userRepository,
                                 PlayerAccessService accessService,
                                 PlayerMapper mapper,
                                 PlayerProfileService profileService) {
        this.guardianRepository = guardianRepository;
        this.consentRepository = consentRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.mapper = mapper;
        this.profileService = profileService;
    }

    @Transactional(readOnly = true)
    public Object get(AppUserPrincipal actor, Long playerId) {
        if (actor == null) {
            throw ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Credenciales invalidas.");
        }
        if (actor.role() == Role.ADMIN) {
            throw ApiException.forbidden("GUARDIAN_ACCESS_DENIED",
                    "No tiene permisos para acceder a los datos del tutor.");
        }
        if (actor.role() == Role.ACADEMY_DIRECTOR || actor.role() == Role.PROFESSOR) {
            PlayerProfile profile = accessService.requireStaff(actor, playerId);
            requireMinor(profile);
            PlayerGuardian guardian = guardianRepository.findByPlayerProfile_Id(profile.getId())
                    .orElseThrow(this::guardianNotFound);
            PlayerGuardianConsentEvent consent = latestConsent(profile.getId());
            return mapper.toGuardianStaff(guardian, consent);
        }
        if (actor.role() == Role.SCOUT) {
            PlayerProfile profile = accessService.requireScoutEligible(actor, playerId);
            if (!PlayerTime.isMinor(profile.getBirthDate(), PlayerTime.today())) {
                throw guardianNotFound();
            }
            PlayerGuardian guardian = guardianRepository.findByPlayerProfile_Id(profile.getId())
                    .orElseThrow(this::guardianNotFound);
            PlayerGuardianConsentEvent consent = latestConsent(profile.getId());
            return mapper.toGuardianScout(guardian, consent);
        }
        throw ApiException.forbidden("GUARDIAN_ACCESS_DENIED",
                "No tiene permisos para acceder a los datos del tutor.");
    }

    @Transactional
    public PlayerGuardianStaffResponse upsert(AppUserPrincipal actor, Long playerId,
                                              PlayerGuardianUpsertRequest request) {
        PlayerProfile profile = accessService.requireStaff(actor, playerId);
        accessService.requireWritableByStaff(actor, profile);
        requireMinor(profile);

        PlayerGuardian guardian = guardianRepository.findByPlayerProfile_IdForUpdate(playerId).orElse(null);
        if (guardian != null) {
            if (request.version() == null || !guardian.getVersion().equals(request.version())) {
                throw ApiException.conflict("STALE_GUARDIAN", "El tutor fue modificado por otra operacion.");
            }
        } else {
            guardian = new PlayerGuardian();
            guardian.setPlayerProfile(profile);
            guardian.setCreatedBy(operator(actor));
        }

        guardian.setFirstName(request.firstName().trim());
        guardian.setLastName(request.lastName().trim());
        guardian.setDocumentType(request.documentType());
        guardian.setDocumentNumber(request.documentNumber().trim().toUpperCase());
        guardian.setPhone(request.phone().trim());
        guardian.setAddress(request.address().trim());
        guardian.setRelationship(request.relationship().trim());
        guardian.setUpdatedBy(operator(actor));
        PlayerGuardian saved = guardianRepository.saveAndFlush(guardian);

        profileService.recalculateProfileStatus(profile, true);
        playerProfileRepository.saveAndFlush(profile);

        PlayerGuardianConsentEvent consent = latestConsent(profile.getId());
        return mapper.toGuardianStaff(saved, consent);
    }

    @Transactional
    public PlayerGuardianStaffResponse updateConsent(AppUserPrincipal actor, Long playerId,
                                                     GuardianConsentUpdateRequest request) {
        accessService.requireStaffRole(actor);
        // Bloquea el perfil antes de leer el consentimiento para serializar la
        // revocacion con el envio de recomendaciones y evitar instantaneas obsoletas.
        PlayerProfile profile = playerProfileRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> ApiException.notFound("PLAYER_NOT_FOUND", "Jugador no encontrado."));
        accessService.requireStaff(actor, profile);
        accessService.requireWritableByStaff(actor, profile);
        requireMinor(profile);
        if (request.status() == GuardianConsentStatus.NOT_RECORDED) {
            throw ApiException.badRequest("INVALID_CONSENT_STATUS", "El estado del consentimiento no es valido.");
        }
        PlayerGuardian guardian = guardianRepository.findByPlayerProfile_Id(profile.getId())
                .orElseThrow(() -> ApiException.conflict("GUARDIAN_REQUIRED", "Debe registrar al tutor antes del consentimiento."));

        PlayerGuardianConsentEvent latest = latestConsent(profile.getId());
        GuardianConsentStatus current = latest == null ? GuardianConsentStatus.NOT_RECORDED : latest.getStatus();
        if (current != request.status() && !isTransitionAllowed(current, request.status())) {
            throw ApiException.conflict("INVALID_CONSENT_TRANSITION",
                    "La transicion de consentimiento solicitada no es valida.");
        }
        if (current == request.status() && latest != null) {
            return mapper.toGuardianStaff(guardian, latest);
        }

        PlayerGuardianConsentEvent event = new PlayerGuardianConsentEvent();
        event.setPlayerProfile(profile);
        event.setStatus(request.status());
        event.setRecordedAt(Instant.now());
        event.setRecordedBy(operator(actor));
        event.setEvidenceUrl(optional(request.evidenceUrl()));
        event.setNote(optional(request.note()));
        PlayerGuardianConsentEvent saved = consentRepository.saveAndFlush(event);
        return mapper.toGuardianStaff(guardian, saved);
    }

    private boolean isTransitionAllowed(GuardianConsentStatus current, GuardianConsentStatus target) {
        return switch (target) {
            case GRANTED -> current == GuardianConsentStatus.NOT_RECORDED
                    || current == GuardianConsentStatus.DENIED
                    || current == GuardianConsentStatus.REVOKED;
            case DENIED -> current == GuardianConsentStatus.NOT_RECORDED
                    || current == GuardianConsentStatus.REVOKED;
            case REVOKED -> current == GuardianConsentStatus.GRANTED;
            case NOT_RECORDED -> false;
        };
    }

    private void requireMinor(PlayerProfile profile) {
        if (!PlayerTime.isMinor(profile.getBirthDate(), PlayerTime.today())) {
            throw ApiException.conflict("GUARDIAN_NOT_APPLICABLE",
                    "El jugador es mayor de edad; los datos del tutor no se exponen.");
        }
    }

    private PlayerGuardianConsentEvent latestConsent(Long profileId) {
        return consentRepository.findTopByPlayerProfile_IdOrderByRecordedAtDescIdDesc(profileId).orElse(null);
    }

    private ApiException guardianNotFound() {
        return ApiException.notFound("GUARDIAN_NOT_FOUND", "Tutor no encontrado.");
    }

    private User operator(AppUserPrincipal actor) {
        return userRepository.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Credenciales invalidas."));
    }

    private String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
