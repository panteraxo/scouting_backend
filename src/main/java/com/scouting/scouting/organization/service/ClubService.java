package com.scouting.scouting.organization.service;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.organization.dto.ClubCreateRequest;
import com.scouting.scouting.organization.dto.ClubResponse;
import com.scouting.scouting.organization.dto.ClubUpdateRequest;
import com.scouting.scouting.organization.dto.VerificationUpdateRequest;
import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.OrganizationType;
import com.scouting.scouting.organization.entity.VerificationStatus;
import com.scouting.scouting.organization.repository.ClubRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.ubigeo.service.UbigeoCatalog;
import com.scouting.scouting.user.entity.Role;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Objects;
import java.util.Set;

/** Reglas de negocio de clubes con el mismo modelo de alcance que academias. */
@Service
public class ClubService {

    private static final String COUNTRY_CODE = "PE";
    private static final Set<Role> ADMIN_ONLY = Set.of(Role.ADMIN);
    private static final Set<Role> MANAGER_ONLY = Set.of(Role.CLUB_MANAGER);

    private final ClubRepository clubRepository;
    private final OrganizationCodeAllocator codeAllocator;
    private final OrganizationScopeService scopeService;
    private final OrganizationMapper mapper;
    private final UbigeoCatalog ubigeoCatalog;

    public ClubService(ClubRepository clubRepository,
                       OrganizationCodeAllocator codeAllocator,
                       OrganizationScopeService scopeService,
                       OrganizationMapper mapper,
                       UbigeoCatalog ubigeoCatalog) {
        this.clubRepository = clubRepository;
        this.codeAllocator = codeAllocator;
        this.scopeService = scopeService;
        this.mapper = mapper;
        this.ubigeoCatalog = ubigeoCatalog;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClubResponse> listAdmin(AppUserPrincipal actor,
                                                int page,
                                                int size,
                                                String sort,
                                                String q,
                                                String type,
                                                String ubigeo,
                                                OperationalStatus operationalStatus,
                                                VerificationStatus verificationStatus) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        Pageable pageable = OrganizationPageRequestFactory.create(
                page, size, sort, "name,asc", OrganizationPageRequestFactory.CLUB_SORT_PROPERTIES);
        Page<Club> result = clubRepository.searchAdmin(
                OrganizationSearchQuery.containsPattern(q), normalizeFilter(type),
                ubigeoCatalog.normalizeUbigeoFilter(ubigeo),
                operationalStatus, verificationStatus, pageable);
        return PageResponse.from(result.map(mapper::toClubResponse));
    }

    @Transactional
    public ClubResponse create(AppUserPrincipal actor, ClubCreateRequest request) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        ClubValues values = values(request);
        validate(values, null);

        Club club = new Club();
        apply(club, values);
        club.setCode(codeAllocator.nextCode(OrganizationType.CLUB));
        club.setCountryCode(COUNTRY_CODE);
        club.setOperationalStatus(OperationalStatus.ACTIVE);
        club.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
        return mapper.toClubResponse(saveClub(club));
    }

    @Transactional(readOnly = true)
    public ClubResponse getAdmin(AppUserPrincipal actor, Long id) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        return mapper.toClubResponse(findById(id));
    }

    @Transactional
    public ClubResponse updateAdmin(AppUserPrincipal actor, Long id, ClubUpdateRequest request) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        Club club = findForUpdate(id);
        ClubValues values = values(request);
        validate(values, id);
        apply(club, values);
        return mapper.toClubResponse(saveClub(club));
    }

    @Transactional
    public ClubResponse deactivateAdmin(AppUserPrincipal actor, Long id) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        Club club = findForUpdate(id);
        if (club.getOperationalStatus() == OperationalStatus.INACTIVE) {
            throw ApiException.conflict("ORGANIZATION_ALREADY_INACTIVE", "El club ya esta inhabilitado.");
        }
        club.setOperationalStatus(OperationalStatus.INACTIVE);
        return mapper.toClubResponse(clubRepository.saveAndFlush(club));
    }

    @Transactional
    public ClubResponse reactivateAdmin(AppUserPrincipal actor, Long id) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        Club club = findForUpdate(id);
        if (club.getOperationalStatus() == OperationalStatus.ACTIVE) {
            throw ApiException.conflict("ORGANIZATION_ALREADY_ACTIVE", "El club ya esta activo.");
        }
        club.setOperationalStatus(OperationalStatus.ACTIVE);
        return mapper.toClubResponse(clubRepository.saveAndFlush(club));
    }

    @Transactional
    public ClubResponse updateVerification(AppUserPrincipal actor, Long id,
                                            VerificationUpdateRequest request) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        if (request == null || request.status() == null) {
            throw ApiException.badRequest("VALIDATION_ERROR", "El estado de verificacion es obligatorio.");
        }
        Club club = findForUpdate(id);
        VerificationStatus current = club.getVerificationStatus();
        VerificationStatus target = request.status();
        if (!isVerificationTransitionAllowed(current, target)) {
            throw ApiException.conflict("INVALID_VERIFICATION_TRANSITION",
                    "La transicion de verificacion solicitada no esta permitida.");
        }
        club.setVerificationStatus(target);
        return mapper.toClubResponse(clubRepository.saveAndFlush(club));
    }

    @Transactional(readOnly = true)
    public ClubResponse getOwn(AppUserPrincipal actor) {
        Club club = scopeService.requireClub(actor, MANAGER_ONLY);
        return mapper.toClubResponse(club);
    }

    @Transactional
    public ClubResponse updateOwn(AppUserPrincipal actor, ClubUpdateRequest request) {
        Club scoped = scopeService.requireClub(actor, MANAGER_ONLY);
        Club club = findForUpdate(scoped.getId());
        if (club.getOperationalStatus() == OperationalStatus.INACTIVE) {
            throw organizationInactive();
        }
        ClubValues values = values(request);
        validate(values, club.getId());
        if (differs(club, values)) {
            apply(club, values);
            club.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
            return mapper.toClubResponse(saveClub(club));
        }
        return mapper.toClubResponse(club);
    }

    @Transactional
    public ClubResponse deactivateOwn(AppUserPrincipal actor) {
        Club scoped = scopeService.requireClub(actor, MANAGER_ONLY);
        Club club = findForUpdate(scoped.getId());
        if (club.getOperationalStatus() == OperationalStatus.INACTIVE) {
            throw ApiException.conflict("ORGANIZATION_ALREADY_INACTIVE", "El club ya esta inhabilitado.");
        }
        club.setOperationalStatus(OperationalStatus.INACTIVE);
        return mapper.toClubResponse(clubRepository.saveAndFlush(club));
    }

    /**
     * Persiste el club traduciendo una colision concurrente del indice unico
     * de RUC. Otros errores de integridad se propagan sin cambios.
     */
    private Club saveClub(Club club) {
        try {
            return clubRepository.saveAndFlush(club);
        } catch (DataIntegrityViolationException ex) {
            ApiException translated = OrganizationDataIntegrity.rucConflictOrNull(
                    ex, OrganizationDataIntegrity.CLUB_RUC_CONSTRAINT,
                    "El RUC ya esta registrado en otro club.");
            if (translated != null) {
                throw translated;
            }
            throw ex;
        }
    }

    private Club findById(Long id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ORGANIZATION_NOT_FOUND", "Organizacion no encontrada."));
    }

    private Club findForUpdate(Long id) {
        return clubRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("ORGANIZATION_NOT_FOUND", "Organizacion no encontrada."));
    }

    private boolean isVerificationTransitionAllowed(VerificationStatus current, VerificationStatus target) {
        return switch (current) {
            case PENDING_VERIFICATION -> target == VerificationStatus.VERIFIED
                    || target == VerificationStatus.REJECTED;
            case REJECTED -> target == VerificationStatus.PENDING_VERIFICATION;
            case VERIFIED -> target == VerificationStatus.PENDING_VERIFICATION;
        };
    }

    private ApiException organizationInactive() {
        return ApiException.conflict("ORGANIZATION_INACTIVE",
                "El club esta inhabilitado y solo admite lectura.");
    }

    private void validate(ClubValues values, Long id) {
        if (values.foundationYear() == null || values.foundationYear() > Year.now().getValue()) {
            throw ApiException.badRequest("INVALID_FOUNDATION_YEAR",
                    "El anio de fundacion no puede ser futuro.");
        }
        if (values.ruc() == null || !values.ruc().matches("\\d{11}")) {
            throw ApiException.badRequest("VALIDATION_ERROR", "El RUC debe tener 11 digitos.");
        }
        ubigeoCatalog.requireDistrict(values.ubigeoCode());
        boolean rucExists = id == null
                ? clubRepository.existsByRuc(values.ruc())
                : clubRepository.existsByRucAndIdNot(values.ruc(), id);
        if (rucExists) {
            throw ApiException.conflict("RUC_ALREADY_EXISTS", "El RUC ya esta registrado en otro club.");
        }
    }

    private void apply(Club club, ClubValues values) {
        club.setName(values.name());
        club.setType(values.type());
        club.setRuc(values.ruc());
        club.setFoundationYear(values.foundationYear());
        club.setLogoUrl(values.logoUrl());
        club.setDescription(values.description());
        club.setUbigeoCode(values.ubigeoCode());
        club.setAddress(values.address());
        club.setInstitutionalEmail(values.institutionalEmail());
        club.setInstitutionalPhone(values.institutionalPhone());
        club.setWebsiteUrl(values.websiteUrl());
        club.setInstagramUrl(values.instagramUrl());
        club.setFacebookUrl(values.facebookUrl());
        club.setYoutubeUrl(values.youtubeUrl());
        club.setTiktokUrl(values.tiktokUrl());
        club.setCountryCode(COUNTRY_CODE);
    }

    private boolean differs(Club club, ClubValues values) {
        return !Objects.equals(club.getName(), values.name())
                || !Objects.equals(club.getType(), values.type())
                || !Objects.equals(club.getRuc(), values.ruc())
                || !Objects.equals(club.getFoundationYear(), values.foundationYear())
                || !Objects.equals(club.getLogoUrl(), values.logoUrl())
                || !Objects.equals(club.getDescription(), values.description())
                || !Objects.equals(club.getUbigeoCode(), values.ubigeoCode())
                || !Objects.equals(club.getAddress(), values.address())
                || !Objects.equals(club.getInstitutionalEmail(), values.institutionalEmail())
                || !Objects.equals(club.getInstitutionalPhone(), values.institutionalPhone())
                || !Objects.equals(club.getWebsiteUrl(), values.websiteUrl())
                || !Objects.equals(club.getInstagramUrl(), values.instagramUrl())
                || !Objects.equals(club.getFacebookUrl(), values.facebookUrl())
                || !Objects.equals(club.getYoutubeUrl(), values.youtubeUrl())
                || !Objects.equals(club.getTiktokUrl(), values.tiktokUrl());
    }

    private ClubValues values(ClubCreateRequest request) {
        return new ClubValues(
                required(request.name()),
                required(request.type()),
                required(request.ruc()),
                request.foundationYear(),
                required(request.logoUrl()),
                required(request.description()),
                request.ubigeoCode(),
                required(request.address()),
                required(request.institutionalEmail()),
                required(request.institutionalPhone()),
                optional(request.websiteUrl()),
                optional(request.instagramUrl()),
                optional(request.facebookUrl()),
                optional(request.youtubeUrl()),
                optional(request.tiktokUrl())
        );
    }

    private ClubValues values(ClubUpdateRequest request) {
        return new ClubValues(
                required(request.name()),
                required(request.type()),
                required(request.ruc()),
                request.foundationYear(),
                required(request.logoUrl()),
                required(request.description()),
                request.ubigeoCode(),
                required(request.address()),
                required(request.institutionalEmail()),
                required(request.institutionalPhone()),
                optional(request.websiteUrl()),
                optional(request.instagramUrl()),
                optional(request.facebookUrl()),
                optional(request.youtubeUrl()),
                optional(request.tiktokUrl())
        );
    }

    private String required(String value) {
        return value == null ? null : value.trim();
    }

    private String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ClubValues(
            String name,
            String type,
            String ruc,
            Integer foundationYear,
            String logoUrl,
            String description,
            String ubigeoCode,
            String address,
            String institutionalEmail,
            String institutionalPhone,
            String websiteUrl,
            String instagramUrl,
            String facebookUrl,
            String youtubeUrl,
            String tiktokUrl
    ) {
    }
}
