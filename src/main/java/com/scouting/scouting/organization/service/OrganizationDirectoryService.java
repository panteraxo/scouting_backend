package com.scouting.scouting.organization.service;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.organization.dto.AcademyDirectoryResponse;
import com.scouting.scouting.organization.dto.ClubDirectoryResponse;
import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.VerificationStatus;
import com.scouting.scouting.organization.repository.AcademyRepository;
import com.scouting.scouting.organization.repository.ClubRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.ubigeo.service.UbigeoCatalog;
import com.scouting.scouting.user.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Directorio de ojeadores. Las consultas fijan siempre ACTIVE + VERIFIED; no
 * existe parametro de estado que pueda ampliar los resultados.
 */
@Service
public class OrganizationDirectoryService {

    private static final Set<Role> DIRECTORY_ROLES = Set.of(Role.ADMIN, Role.SCOUT);

    private final AcademyRepository academyRepository;
    private final ClubRepository clubRepository;
    private final OrganizationScopeService scopeService;
    private final OrganizationMapper mapper;
    private final UbigeoCatalog ubigeoCatalog;

    public OrganizationDirectoryService(AcademyRepository academyRepository,
                                        ClubRepository clubRepository,
                                        OrganizationScopeService scopeService,
                                        OrganizationMapper mapper,
                                        UbigeoCatalog ubigeoCatalog) {
        this.academyRepository = academyRepository;
        this.clubRepository = clubRepository;
        this.scopeService = scopeService;
        this.mapper = mapper;
        this.ubigeoCatalog = ubigeoCatalog;
    }

    @Transactional(readOnly = true)
    public PageResponse<AcademyDirectoryResponse> listAcademies(AppUserPrincipal actor,
                                                                int page,
                                                                int size,
                                                                String sort,
                                                                String q,
                                                                String type,
                                                                String ubigeo) {
        scopeService.requireRole(actor, DIRECTORY_ROLES);
        Pageable pageable = OrganizationPageRequestFactory.create(
                page, size, sort, "name,asc", OrganizationPageRequestFactory.ACADEMY_SORT_PROPERTIES);
        Page<Academy> result = academyRepository.searchDirectory(
                OperationalStatus.ACTIVE,
                VerificationStatus.VERIFIED,
                OrganizationSearchQuery.containsPattern(q),
                normalizeFilter(type),
                ubigeoCatalog.normalizeUbigeoFilter(ubigeo),
                pageable);
        return PageResponse.from(result.map(mapper::toAcademyDirectoryResponse));
    }

    @Transactional(readOnly = true)
    public AcademyDirectoryResponse getAcademy(AppUserPrincipal actor, Long id) {
        scopeService.requireRole(actor, DIRECTORY_ROLES);
        Academy academy = academyRepository.findByIdAndOperationalStatusAndVerificationStatus(
                        id, OperationalStatus.ACTIVE, VerificationStatus.VERIFIED)
                .orElseThrow(this::notFound);
        return mapper.toAcademyDirectoryResponse(academy);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClubDirectoryResponse> listClubs(AppUserPrincipal actor,
                                                         int page,
                                                         int size,
                                                         String sort,
                                                         String q,
                                                         String type,
                                                         String ubigeo) {
        scopeService.requireRole(actor, DIRECTORY_ROLES);
        Pageable pageable = OrganizationPageRequestFactory.create(
                page, size, sort, "name,asc", OrganizationPageRequestFactory.CLUB_SORT_PROPERTIES);
        Page<Club> result = clubRepository.searchDirectory(
                OperationalStatus.ACTIVE,
                VerificationStatus.VERIFIED,
                OrganizationSearchQuery.containsPattern(q),
                normalizeFilter(type),
                ubigeoCatalog.normalizeUbigeoFilter(ubigeo),
                pageable);
        return PageResponse.from(result.map(mapper::toClubDirectoryResponse));
    }

    @Transactional(readOnly = true)
    public ClubDirectoryResponse getClub(AppUserPrincipal actor, Long id) {
        scopeService.requireRole(actor, DIRECTORY_ROLES);
        Club club = clubRepository.findByIdAndOperationalStatusAndVerificationStatus(
                        id, OperationalStatus.ACTIVE, VerificationStatus.VERIFIED)
                .orElseThrow(this::notFound);
        return mapper.toClubDirectoryResponse(club);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ApiException notFound() {
        return ApiException.notFound("ORGANIZATION_NOT_FOUND", "Organizacion no encontrada.");
    }
}
