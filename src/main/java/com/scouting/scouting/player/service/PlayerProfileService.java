package com.scouting.scouting.player.service;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.player.dto.PlayerAccountCandidateResponse;
import com.scouting.scouting.player.dto.PlayerAdminDetailResponse;
import com.scouting.scouting.player.dto.PlayerCreateRequest;
import com.scouting.scouting.player.dto.PlayerDirectoryItemResponse;
import com.scouting.scouting.player.dto.PlayerOwnResponse;
import com.scouting.scouting.player.dto.PlayerScoutDetailResponse;
import com.scouting.scouting.player.dto.PlayerStaffDetailResponse;
import com.scouting.scouting.player.dto.PlayerStatusUpdateRequest;
import com.scouting.scouting.player.dto.PlayerUpdateRequest;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.entity.PlayerProfileStatus;
import com.scouting.scouting.player.entity.PositionGroup;
import com.scouting.scouting.player.repository.PlayerGuardianRepository;
import com.scouting.scouting.player.repository.PlayerHistoryEntryRepository;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.ubigeo.service.UbigeoCatalog;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import com.scouting.scouting.user.service.UserPageRequestFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Servicio principal de perfiles de jugador (SCP-005). */
@Service
public class PlayerProfileService {

    private static final Set<Role> LIST_ROLES = Set.of(
            Role.ADMIN, Role.ACADEMY_DIRECTOR, Role.PROFESSOR, Role.SCOUT);
    private static final Set<Role> CREATE_ROLES = Set.of(
            Role.ADMIN, Role.ACADEMY_DIRECTOR, Role.PROFESSOR);

    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerGuardianRepository playerGuardianRepository;
    private final PlayerHistoryEntryRepository historyRepository;
    private final UserRepository userRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final PlayerAccessService accessService;
    private final PlayerMapper mapper;
    private final UbigeoCatalog ubigeoCatalog;

    public PlayerProfileService(PlayerProfileRepository playerProfileRepository,
                                PlayerGuardianRepository playerGuardianRepository,
                                PlayerHistoryEntryRepository historyRepository,
                                UserRepository userRepository,
                                UserOrganizationRepository userOrganizationRepository,
                                PlayerAccessService accessService,
                                PlayerMapper mapper,
                                UbigeoCatalog ubigeoCatalog) {
        this.playerProfileRepository = playerProfileRepository;
        this.playerGuardianRepository = playerGuardianRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.userOrganizationRepository = userOrganizationRepository;
        this.accessService = accessService;
        this.mapper = mapper;
        this.ubigeoCatalog = ubigeoCatalog;
    }

    @Transactional(readOnly = true)
    public PageResponse<PlayerDirectoryItemResponse> list(AppUserPrincipal actor,
                                                          int page,
                                                          int size,
                                                          String sort,
                                                          String q,
                                                          PlayerPosition position,
                                                          PositionGroup positionGroup,
                                                          Integer ageMin,
                                                          Integer ageMax,
                                                          String regionCode,
                                                          String provinceCode,
                                                          String districtCode,
                                                          Long academyId,
                                                          PlayerOperationalStatus operationalStatus,
                                                          PlayerProfileStatus profileStatus) {
        accessService.requireRole(actor, LIST_ROLES);
        boolean scout = actor.role() == Role.SCOUT;
        if (scout && (operationalStatus != null || profileStatus != null)) {
            throw forbidden();
        }
        if (scout && districtCode != null) {
            throw forbidden();
        }
        if (scout && sort != null && sort.trim().toLowerCase(java.util.Locale.ROOT)
                .startsWith("birthdate")) {
            throw ApiException.badRequest("INVALID_SORT",
                    "El ojeador no puede ordenar por fecha de nacimiento.");
        }

        Long scopedAcademyId = null;
        if (actor.role() == Role.PROFESSOR || actor.role() == Role.ACADEMY_DIRECTOR) {
            Academy academy = accessService.requireActorAcademy(actor);
            if (academyId != null && !academyId.equals(academy.getId())) {
                throw forbidden();
            }
            scopedAcademyId = academy.getId();
        }

        validateAgeRange(ageMin, ageMax);
        LocalDate today = PlayerTime.today();
        LocalDate minBirthDate = ageMin == null ? null : PlayerTime.minBirthDateForAge(ageMin, today);
        LocalDate maxBirthDateExclusive = ageMax == null
                ? null : PlayerTime.maxBirthDateExclusiveForAge(ageMax, today);
        String residencePrefix = normalizeResidenceFilter(regionCode, provinceCode, districtCode, scout);

        Collection<PlayerPosition> positions = resolvePositions(position, positionGroup);
        Pageable pageable = PlayerPageRequestFactory.create(page, size, sort);

        PlayerOperationalStatus effectiveOperational = scout ? PlayerOperationalStatus.ACTIVE : operationalStatus;
        PlayerProfileStatus effectiveProfile = scout ? PlayerProfileStatus.COMPLETE : profileStatus;

        Page<PlayerProfile> result = playerProfileRepository.search(
                PlayerSearchQuery.containsPattern(q),
                positions,
                minBirthDate,
                maxBirthDateExclusive,
                residencePrefix,
                academyId,
                scopedAcademyId,
                effectiveOperational,
                effectiveProfile,
                scout,
                UserStatus.ACTIVE,
                scout,
                com.scouting.scouting.organization.entity.OperationalStatus.ACTIVE,
                pageable);

        Map<Long, UserOrganization> organizations = organizationsByUser(result.getContent());
        Map<Long, Integer> years = mapper.yearsExperienceByProfileIds(
                result.getContent().stream().map(PlayerProfile::getId).toList());
        return PageResponse.from(result.map(profile -> scout
                ? mapper.toScoutDirectory(
                        profile,
                        organizations.get(profile.getUser().getId()),
                        years.get(profile.getId()))
                : mapper.toDirectory(
                        profile,
                        organizations.get(profile.getUser().getId()),
                        years.get(profile.getId()))));
    }

    @Transactional(readOnly = true)
    public PageResponse<PlayerAccountCandidateResponse> accountCandidates(AppUserPrincipal actor,
                                                                          int page,
                                                                          int size,
                                                                          String sort,
                                                                          String q,
                                                                          Long academyId) {
        accessService.requireRole(actor, CREATE_ROLES);
        Long effectiveAcademyId = academyId;
        if (actor.role() == Role.PROFESSOR || actor.role() == Role.ACADEMY_DIRECTOR) {
            Academy academy = accessService.requireActorAcademy(actor);
            if (academyId != null && !academyId.equals(academy.getId())) {
                throw forbidden();
            }
            effectiveAcademyId = academy.getId();
        }
        Pageable pageable = UserPageRequestFactory.create(page, size,
                sort == null || sort.isBlank() ? "lastName,asc" : sort);
        Page<User> result = playerProfileRepository.searchAccountCandidates(
                Role.PLAYER,
                UserStatus.ACTIVE,
                effectiveAcademyId,
                PlayerSearchQuery.containsPattern(q),
                pageable);
        return PageResponse.from(result.map(user ->
                new PlayerAccountCandidateResponse(user.getId(), user.getFirstName(), user.getLastName())));
    }

    @Transactional(readOnly = true)
    public Object get(AppUserPrincipal actor, Long id) {
        PlayerProfile profile = accessService.requireView(actor, id);
        UserOrganization organization = accessService.organization(profile);
        return switch (actor.role()) {
            case ADMIN -> mapper.toAdmin(profile, organization);
            case ACADEMY_DIRECTOR, PROFESSOR -> mapper.toStaff(profile, organization);
            case SCOUT -> mapper.toScout(profile, organization);
            case PLAYER -> mapper.toOwn(profile, organization);
            default -> throw forbidden();
        };
    }

    @Transactional(readOnly = true)
    public PlayerOwnResponse getMe(AppUserPrincipal actor) {
        PlayerProfile profile = accessService.requireOwn(actor);
        return mapper.toOwn(profile, accessService.organization(profile));
    }

    @Transactional
    public Object create(AppUserPrincipal actor, PlayerCreateRequest request) {
        accessService.requireRole(actor, CREATE_ROLES);
        User user = userRepository.findByIdForUpdate(request.userId())
                .orElseThrow(() -> ApiException.notFound("PLAYER_ACCOUNT_NOT_FOUND", "Cuenta de jugador no encontrada."));
        UserOrganization organization = userOrganizationRepository.findByUser_Id(user.getId()).orElse(null);

        // El staff no debe poder enumerar cuentas fuera de su academia. Se resuelve
        // el alcance antes de revelar rol, estado o existencia de perfil.
        if (actor.role() != Role.ADMIN) {
            Academy academy = accessService.requireActorAcademy(actor);
            if (organization == null || organization.getAcademy() == null
                    || !academy.getId().equals(organization.getAcademy().getId())) {
                throw playerAccountNotFound();
            }
        }

        if (user.getRole() != Role.PLAYER) {
            throw ApiException.conflict("PLAYER_ACCOUNT_REQUIRED", "La cuenta no tiene rol de jugador.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.conflict("PLAYER_ACCOUNT_INACTIVE", "La cuenta del jugador no esta activa.");
        }
        if (organization == null || organization.getAcademy() == null) {
            throw ApiException.conflict("PLAYER_WITHOUT_ACADEMY",
                    "La cuenta no tiene una academia asignada.");
        }
        accessService.requireAcademyActiveForCreate(organization);
        if (playerProfileRepository.existsByUser_Id(user.getId())) {
            throw ApiException.conflict("PLAYER_PROFILE_ALREADY_EXISTS", "La cuenta ya tiene un perfil deportivo.");
        }

        LocalDate birthDate = validateBirthDate(request.birthDate());
        String residence = normalizeResidence(request.residenceUbigeoCode());
        User operator = operator(actor);
        PlayerProfile profile = new PlayerProfile();
        profile.setUser(user);
        profile.setBirthDate(birthDate);
        profile.setResidenceUbigeoCode(residence);
        profile.setHeightCm(request.heightCm());
        profile.setWeightKg(request.weightKg());
        profile.setPosition(request.position());
        profile.setBestPosition(request.bestPosition());
        profile.setDominantFoot(request.dominantFoot());
        profile.setSummary(optional(request.summary()));
        profile.setPhotoUrl(optional(request.photoUrl()));
        profile.setOperationalStatus(PlayerOperationalStatus.ACTIVE);
        profile.setCreatedBy(operator);
        profile.setUpdatedBy(operator);
        recalculateProfileStatus(profile, false);
        PlayerProfile saved = playerProfileRepository.saveAndFlush(profile);
        UserOrganization savedOrganization = userOrganizationRepository.findByUser_Id(saved.getUser().getId())
                .orElseThrow(() -> ApiException.conflict("PLAYER_WITHOUT_ACADEMY",
                        "La cuenta no tiene una academia asignada."));
        return actor.role() == Role.ADMIN
                ? mapper.toAdmin(saved, savedOrganization)
                : mapper.toStaff(saved, savedOrganization);
    }

    @Transactional
    public Object update(AppUserPrincipal actor, Long id, PlayerUpdateRequest request) {
        accessService.requireManage(actor, id);
        PlayerProfile profile = lockProfile(id);
        accessService.requireWritableByStaff(actor, profile);
        if (!profile.getVersion().equals(request.version())) {
            throw ApiException.conflict("STALE_PLAYER_PROFILE", "El perfil fue modificado por otra operacion.");
        }
        LocalDate birthDate = validateBirthDate(request.birthDate());
        LocalDate firstActivityDate = historyRepository.findFirstActivityDate(profile.getId());
        if (firstActivityDate != null && birthDate.isAfter(firstActivityDate)) {
            throw ApiException.badRequest("BIRTH_DATE_AFTER_HISTORY",
                    "La fecha de nacimiento no puede ser posterior a la primera actividad registrada.");
        }
        String residence = normalizeResidence(request.residenceUbigeoCode());
        profile.setBirthDate(birthDate);
        profile.setResidenceUbigeoCode(residence);
        profile.setHeightCm(request.heightCm());
        profile.setWeightKg(request.weightKg());
        profile.setPosition(request.position());
        profile.setBestPosition(request.bestPosition());
        profile.setDominantFoot(request.dominantFoot());
        profile.setSummary(optional(request.summary()));
        profile.setPhotoUrl(optional(request.photoUrl()));
        profile.setUpdatedBy(operator(actor));
        recalculateProfileStatus(profile, playerGuardianRepository.existsByPlayerProfile_Id(profile.getId()));
        PlayerProfile saved = playerProfileRepository.saveAndFlush(profile);
        UserOrganization organization = accessService.organization(saved);
        return actor.role() == Role.ADMIN
                ? mapper.toAdmin(saved, organization)
                : mapper.toStaff(saved, organization);
    }

    @Transactional
    public Object updateStatus(AppUserPrincipal actor, Long id, PlayerStatusUpdateRequest request) {
        accessService.requireManage(actor, id);
        PlayerProfile profile = lockProfile(id);
        if (actor.role() != Role.ADMIN) {
            if (profile.getOperationalStatus() == PlayerOperationalStatus.INACTIVE
                    && request.status() == PlayerOperationalStatus.ACTIVE) {
                throw ApiException.conflict("ONLY_ADMIN_CAN_REACTIVATE",
                        "Solo un administrador puede reactivar un perfil.");
            }
            if (request.status() == PlayerOperationalStatus.INACTIVE) {
                accessService.requireWritableByStaff(actor, profile);
            }
        }
        profile.setOperationalStatus(request.status());
        profile.setUpdatedBy(operator(actor));
        PlayerProfile saved = playerProfileRepository.saveAndFlush(profile);
        UserOrganization organization = accessService.organization(saved);
        return actor.role() == Role.ADMIN
                ? mapper.toAdmin(saved, organization)
                : mapper.toStaff(saved, organization);
    }

    private PlayerProfile lockProfile(Long id) {
        return playerProfileRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("PLAYER_NOT_FOUND", "Jugador no encontrado."));
    }

    private ApiException playerAccountNotFound() {
        return ApiException.notFound("PLAYER_ACCOUNT_NOT_FOUND", "Cuenta de jugador no encontrada.");
    }

    /** Recalcula completitud y completedAt. Debe invocarse dentro de una transaccion. */
    public void recalculateProfileStatus(PlayerProfile profile, boolean guardianExists) {
        boolean complete = profile.getBirthDate() != null
                && profile.getResidenceUbigeoCode() != null
                && profile.getPosition() != null
                && profile.getDominantFoot() != null
                && profile.getHeightCm() != null
                && profile.getWeightKg() != null;
        if (complete && PlayerTime.isMinor(profile.getBirthDate(), PlayerTime.today()) && !guardianExists) {
            complete = false;
        }
        if (complete) {
            profile.setProfileStatus(PlayerProfileStatus.COMPLETE);
            if (profile.getCompletedAt() == null) {
                profile.setCompletedAt(Instant.now());
            }
        } else {
            profile.setProfileStatus(PlayerProfileStatus.INCOMPLETE);
            profile.setCompletedAt(null);
        }
    }

    private LocalDate validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw ApiException.badRequest("VALIDATION_ERROR", "La fecha de nacimiento es obligatoria.");
        }
        LocalDate today = PlayerTime.today();
        if (birthDate.isAfter(today)) {
            throw ApiException.badRequest("INVALID_BIRTH_DATE", "La fecha de nacimiento no puede ser futura.");
        }
        if (birthDate.isBefore(LocalDate.of(1900, 1, 1))) {
            throw ApiException.badRequest("INVALID_BIRTH_DATE", "La fecha de nacimiento no es valida.");
        }
        return birthDate;
    }

    private String normalizeResidence(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw ApiException.badRequest("INVALID_RESIDENCE_UBIGEO", "El UBIGEO de residencia es obligatorio.");
        }
        try {
            return ubigeoCatalog.requireDistrict(rawCode.trim()).districtCode();
        } catch (ApiException ex) {
            throw ApiException.badRequest("INVALID_RESIDENCE_UBIGEO",
                    "El UBIGEO de residencia no existe en el catalogo.");
        }
    }

    private String normalizeResidenceFilter(String regionCode, String provinceCode,
                                            String districtCode, boolean scout) {
        if (districtCode != null) {
            try {
                ubigeoCatalog.requireDistrict(districtCode);
            } catch (ApiException ex) {
                throw ApiException.badRequest("INVALID_LOCATION_FILTER", "El distrito no es valido.");
            }
            if (provinceCode != null && !districtCode.startsWith(provinceCode)) {
                throw ApiException.badRequest("INVALID_LOCATION_FILTER", "La jerarquia de ubicacion no es valida.");
            }
            if (regionCode != null && !districtCode.startsWith(regionCode)) {
                throw ApiException.badRequest("INVALID_LOCATION_FILTER", "La jerarquia de ubicacion no es valida.");
            }
            return districtCode;
        }
        if (provinceCode != null) {
            if (!provinceCode.matches("\\d{4}")) {
                throw ApiException.badRequest("INVALID_LOCATION_FILTER", "La provincia no es valida.");
            }
            if (regionCode != null && !provinceCode.startsWith(regionCode)) {
                throw ApiException.badRequest("INVALID_LOCATION_FILTER", "La jerarquia de ubicacion no es valida.");
            }
            return provinceCode;
        }
        if (regionCode != null) {
            if (!regionCode.matches("\\d{2}")) {
                throw ApiException.badRequest("INVALID_LOCATION_FILTER", "La region no es valida.");
            }
            return regionCode;
        }
        return null;
    }

    private void validateAgeRange(Integer ageMin, Integer ageMax) {
        if (ageMin != null && (ageMin < 0 || ageMin > 100)) {
            throw ApiException.badRequest("INVALID_AGE_RANGE", "La edad minima no es valida.");
        }
        if (ageMax != null && (ageMax < 0 || ageMax > 100)) {
            throw ApiException.badRequest("INVALID_AGE_RANGE", "La edad maxima no es valida.");
        }
        if (ageMin != null && ageMax != null && ageMin > ageMax) {
            throw ApiException.badRequest("INVALID_AGE_RANGE", "La edad minima no puede superar la maxima.");
        }
    }

    private Collection<PlayerPosition> resolvePositions(PlayerPosition position, PositionGroup group) {
        if (position != null) {
            return List.of(position);
        }
        if (group != null) {
            return List.of(PlayerPosition.values()).stream()
                    .filter(candidate -> candidate.group() == group)
                    .toList();
        }
        return List.of(PlayerPosition.values());
    }

    private User operator(AppUserPrincipal actor) {
        return userRepository.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Credenciales invalidas."));
    }

    private Map<Long, UserOrganization> organizationsByUser(Collection<PlayerProfile> profiles) {
        if (profiles.isEmpty()) {
            return Map.of();
        }
        List<Long> userIds = profiles.stream().map(profile -> profile.getUser().getId()).toList();
        return userOrganizationRepository.findByUser_IdIn(userIds).stream()
                .collect(Collectors.toMap(
                        association -> association.getUser().getId(),
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new));
    }

    private String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ApiException forbidden() {
        return ApiException.forbidden("ACCESS_DENIED", "No tiene permisos para realizar esta accion.");
    }
}
