package com.scouting.scouting.organization.service;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.organization.dto.AcademyCreateRequest;
import com.scouting.scouting.organization.dto.AcademyResponse;
import com.scouting.scouting.organization.dto.AcademyUpdateRequest;
import com.scouting.scouting.organization.dto.VerificationUpdateRequest;
import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.OrganizationType;
import com.scouting.scouting.organization.entity.VerificationStatus;
import com.scouting.scouting.organization.repository.AcademyRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.ubigeo.service.UbigeoCatalog;
import com.scouting.scouting.user.entity.Role;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Reglas de negocio de academias. La autorizacion se comprueba aqui ademas de
 * en los controladores; el alcance propio se resuelve desde la cuenta
 * autenticada y nunca desde un id enviado por el cliente.
 */
@Service
public class AcademyService {

    private static final Set<Role> ADMIN_ONLY = Set.of(Role.ADMIN);
    private static final Set<Role> OWN_READ_ROLES = Set.of(Role.ACADEMY_DIRECTOR, Role.PROFESSOR);
    private static final Set<Role> DIRECTOR_ONLY = Set.of(Role.ACADEMY_DIRECTOR);

    private final AcademyRepository academyRepository;
    private final OrganizationCodeAllocator codeAllocator;
    private final OrganizationScopeService scopeService;
    private final OrganizationMapper mapper;
    private final UbigeoCatalog ubigeoCatalog;

    public AcademyService(AcademyRepository academyRepository,
                          OrganizationCodeAllocator codeAllocator,
                          OrganizationScopeService scopeService,
                          OrganizationMapper mapper,
                          UbigeoCatalog ubigeoCatalog) {
        this.academyRepository = academyRepository;
        this.codeAllocator = codeAllocator;
        this.scopeService = scopeService;
        this.mapper = mapper;
        this.ubigeoCatalog = ubigeoCatalog;
    }

    @Transactional(readOnly = true)
    public PageResponse<AcademyResponse> listAdmin(AppUserPrincipal actor,
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
                page, size, sort, "name,asc", OrganizationPageRequestFactory.ACADEMY_SORT_PROPERTIES);
        Page<Academy> result = academyRepository.searchAdmin(
                OrganizationSearchQuery.containsPattern(q), normalizeFilter(type),
                ubigeoCatalog.normalizeUbigeoFilter(ubigeo),
                operationalStatus, verificationStatus, pageable);
        return PageResponse.from(result.map(mapper::toAcademyResponse));
    }

    @Transactional
    public AcademyResponse create(AppUserPrincipal actor, AcademyCreateRequest request) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        AcademyValues values = values(request);
        validate(values, null);

        Academy academy = new Academy();
        apply(academy, values);
        academy.setCode(codeAllocator.nextCode(OrganizationType.ACADEMY));
        academy.setOperationalStatus(OperationalStatus.ACTIVE);
        academy.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
        return mapper.toAcademyResponse(saveAcademy(academy));
    }

    @Transactional(readOnly = true)
    public AcademyResponse getAdmin(AppUserPrincipal actor, Long id) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        return mapper.toAcademyResponse(findById(id));
    }

    @Transactional
    public AcademyResponse updateAdmin(AppUserPrincipal actor, Long id, AcademyUpdateRequest request) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        Academy academy = findForUpdate(id);
        AcademyValues values = values(request);
        validate(values, id);
        apply(academy, values);
        // Una edicion administrativa no reinicia la verificacion.
        return mapper.toAcademyResponse(saveAcademy(academy));
    }

    @Transactional
    public AcademyResponse deactivateAdmin(AppUserPrincipal actor, Long id) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        Academy academy = findForUpdate(id);
        if (academy.getOperationalStatus() == OperationalStatus.INACTIVE) {
            throw ApiException.conflict("ORGANIZATION_ALREADY_INACTIVE", "La academia ya esta inhabilitada.");
        }
        academy.setOperationalStatus(OperationalStatus.INACTIVE);
        return mapper.toAcademyResponse(academyRepository.saveAndFlush(academy));
    }

    @Transactional
    public AcademyResponse reactivateAdmin(AppUserPrincipal actor, Long id) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        Academy academy = findForUpdate(id);
        if (academy.getOperationalStatus() == OperationalStatus.ACTIVE) {
            throw ApiException.conflict("ORGANIZATION_ALREADY_ACTIVE", "La academia ya esta activa.");
        }
        academy.setOperationalStatus(OperationalStatus.ACTIVE);
        return mapper.toAcademyResponse(academyRepository.saveAndFlush(academy));
    }

    @Transactional
    public AcademyResponse updateVerification(AppUserPrincipal actor, Long id,
                                               VerificationUpdateRequest request) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        if (request == null || request.status() == null) {
            throw ApiException.badRequest("VALIDATION_ERROR", "El estado de verificacion es obligatorio.");
        }
        Academy academy = findForUpdate(id);
        VerificationStatus current = academy.getVerificationStatus();
        VerificationStatus target = request.status();
        if (!isVerificationTransitionAllowed(current, target)) {
            throw ApiException.conflict("INVALID_VERIFICATION_TRANSITION",
                    "La transicion de verificacion solicitada no esta permitida.");
        }
        academy.setVerificationStatus(target);
        return mapper.toAcademyResponse(academyRepository.saveAndFlush(academy));
    }

    @Transactional(readOnly = true)
    public AcademyResponse getOwn(AppUserPrincipal actor) {
        Academy academy = scopeService.requireAcademy(actor, OWN_READ_ROLES);
        return mapper.toAcademyResponse(academy);
    }

    @Transactional
    public AcademyResponse updateOwn(AppUserPrincipal actor, AcademyUpdateRequest request) {
        Academy scoped = scopeService.requireAcademy(actor, DIRECTOR_ONLY);
        Academy academy = findForUpdate(scoped.getId());
        if (academy.getOperationalStatus() == OperationalStatus.INACTIVE) {
            throw organizationInactive();
        }
        AcademyValues values = values(request);
        validate(values, academy.getId());
        if (differs(academy, values)) {
            apply(academy, values);
            // Todo cambio de contenido realizado por el Director reinicia la verificacion.
            academy.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
            return mapper.toAcademyResponse(saveAcademy(academy));
        }
        return mapper.toAcademyResponse(academy);
    }

    @Transactional
    public AcademyResponse deactivateOwn(AppUserPrincipal actor) {
        Academy scoped = scopeService.requireAcademy(actor, DIRECTOR_ONLY);
        Academy academy = findForUpdate(scoped.getId());
        if (academy.getOperationalStatus() == OperationalStatus.INACTIVE) {
            throw ApiException.conflict("ORGANIZATION_ALREADY_INACTIVE", "La academia ya esta inhabilitada.");
        }
        academy.setOperationalStatus(OperationalStatus.INACTIVE);
        return mapper.toAcademyResponse(academyRepository.saveAndFlush(academy));
    }

    /**
     * Persiste la academia traduciendo una colision concurrente del indice
     * unico de RUC. Otros errores de integridad se propagan sin cambios.
     */
    private Academy saveAcademy(Academy academy) {
        try {
            return academyRepository.saveAndFlush(academy);
        } catch (DataIntegrityViolationException ex) {
            ApiException translated = OrganizationDataIntegrity.rucConflictOrNull(
                    ex, OrganizationDataIntegrity.ACADEMY_RUC_CONSTRAINT,
                    "El RUC ya esta registrado en otra academia.");
            if (translated != null) {
                throw translated;
            }
            throw ex;
        }
    }

    private Academy findById(Long id) {
        return academyRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ORGANIZATION_NOT_FOUND", "Organizacion no encontrada."));
    }

    private Academy findForUpdate(Long id) {
        return academyRepository.findByIdForUpdate(id)
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
                "La academia esta inhabilitada y solo admite lectura.");
    }

    private void validate(AcademyValues values, Long id) {
        if (values.foundationYear() == null || values.foundationYear() > Year.now().getValue()) {
            throw ApiException.badRequest("INVALID_FOUNDATION_YEAR",
                    "El anio de fundacion no puede ser futuro.");
        }
        if (values.ruc() == null || !values.ruc().matches("\\d{11}")) {
            throw ApiException.badRequest("VALIDATION_ERROR", "El RUC debe tener 11 digitos.");
        }
        ubigeoCatalog.requireDistrict(values.ubigeoCode());
        boolean rucExists = id == null
                ? academyRepository.existsByRuc(values.ruc())
                : academyRepository.existsByRucAndIdNot(values.ruc(), id);
        if (rucExists) {
            throw ApiException.conflict("RUC_ALREADY_EXISTS", "El RUC ya esta registrado en otra academia.");
        }
    }

    private void apply(Academy academy, AcademyValues values) {
        academy.setName(values.name());
        academy.setLogoUrl(values.logoUrl());
        academy.setType(values.type());
        academy.setDescription(values.description());
        academy.setFoundationYear(values.foundationYear());
        academy.setRuc(values.ruc());
        academy.setUbigeoCode(values.ubigeoCode());
        academy.setAddress(values.address());
        academy.setInstitutionalEmail(values.institutionalEmail());
        academy.setInstitutionalPhone(values.institutionalPhone());
        academy.setWebsiteUrl(values.websiteUrl());
        academy.setInstagramUrl(values.instagramUrl());
        academy.setFacebookUrl(values.facebookUrl());
        academy.setYoutubeUrl(values.youtubeUrl());
        academy.setTiktokUrl(values.tiktokUrl());
        academy.getTrainingGroups().clear();
        academy.getTrainingGroups().addAll(values.trainingGroups());
        academy.setHeadquarters(values.headquarters());
        academy.setRepresentativeName(values.representativeName() == null ? "" : values.representativeName());
    }

    private boolean differs(Academy academy, AcademyValues values) {
        return !Objects.equals(academy.getName(), values.name())
                || !Objects.equals(academy.getLogoUrl(), values.logoUrl())
                || !Objects.equals(academy.getType(), values.type())
                || !Objects.equals(academy.getDescription(), values.description())
                || !Objects.equals(academy.getFoundationYear(), values.foundationYear())
                || !Objects.equals(academy.getRuc(), values.ruc())
                || !Objects.equals(academy.getUbigeoCode(), values.ubigeoCode())
                || !Objects.equals(academy.getAddress(), values.address())
                || !Objects.equals(academy.getInstitutionalEmail(), values.institutionalEmail())
                || !Objects.equals(academy.getInstitutionalPhone(), values.institutionalPhone())
                || !Objects.equals(academy.getWebsiteUrl(), values.websiteUrl())
                || !Objects.equals(academy.getInstagramUrl(), values.instagramUrl())
                || !Objects.equals(academy.getFacebookUrl(), values.facebookUrl())
                || !Objects.equals(academy.getYoutubeUrl(), values.youtubeUrl())
                || !Objects.equals(academy.getTiktokUrl(), values.tiktokUrl())
                || !Objects.equals(academy.getTrainingGroups(), values.trainingGroups())
                || !Objects.equals(academy.getHeadquarters(), values.headquarters())
                || !Objects.equals(blankToNull(academy.getRepresentativeName()),
                        blankToNull(values.representativeName()));
    }

    private AcademyValues values(AcademyCreateRequest request) {
        return new AcademyValues(
                required(request.name()),
                required(request.logoUrl()),
                required(request.type()),
                required(request.description()),
                request.foundationYear(),
                required(request.ruc()),
                request.ubigeoCode(),
                required(request.address()),
                required(request.institutionalEmail()),
                required(request.institutionalPhone()),
                optional(request.websiteUrl()),
                optional(request.instagramUrl()),
                optional(request.facebookUrl()),
                optional(request.youtubeUrl()),
                optional(request.tiktokUrl()),
                groups(request.trainingGroups()),
                required(request.headquarters()),
                optional(request.representativeName())
        );
    }

    private AcademyValues values(AcademyUpdateRequest request) {
        return new AcademyValues(
                required(request.name()),
                required(request.logoUrl()),
                required(request.type()),
                required(request.description()),
                request.foundationYear(),
                required(request.ruc()),
                request.ubigeoCode(),
                required(request.address()),
                required(request.institutionalEmail()),
                required(request.institutionalPhone()),
                optional(request.websiteUrl()),
                optional(request.instagramUrl()),
                optional(request.facebookUrl()),
                optional(request.youtubeUrl()),
                optional(request.tiktokUrl()),
                groups(request.trainingGroups()),
                required(request.headquarters()),
                optional(request.representativeName())
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

    /**
     * Trata null y cadena en blanco como equivalentes para no reiniciar la
     * verificacion cuando el representante legacy no se informa.
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> groups(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim());
            }
        }
        return List.copyOf(new ArrayList<>(normalized));
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record AcademyValues(
            String name,
            String logoUrl,
            String type,
            String description,
            Integer foundationYear,
            String ruc,
            String ubigeoCode,
            String address,
            String institutionalEmail,
            String institutionalPhone,
            String websiteUrl,
            String instagramUrl,
            String facebookUrl,
            String youtubeUrl,
            String tiktokUrl,
            List<String> trainingGroups,
            String headquarters,
            String representativeName
    ) {
    }
}
