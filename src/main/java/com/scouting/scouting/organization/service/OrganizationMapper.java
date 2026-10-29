package com.scouting.scouting.organization.service;

import com.scouting.scouting.organization.dto.AcademyDirectoryResponse;
import com.scouting.scouting.organization.dto.AcademyResponse;
import com.scouting.scouting.organization.dto.ClubDirectoryResponse;
import com.scouting.scouting.organization.dto.ClubResponse;
import com.scouting.scouting.organization.dto.OrganizationCountsResponse;
import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.ubigeo.service.UbigeoCatalog;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.UserStatus;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Conversion explicita de entidades a DTOs; nunca se serializa una entidad. */
@Component
public class OrganizationMapper {

    private final UbigeoCatalog ubigeoCatalog;
    private final UserOrganizationRepository userOrganizationRepository;

    public OrganizationMapper(UbigeoCatalog ubigeoCatalog,
                              UserOrganizationRepository userOrganizationRepository) {
        this.ubigeoCatalog = ubigeoCatalog;
        this.userOrganizationRepository = userOrganizationRepository;
    }

    public AcademyResponse toAcademyResponse(Academy academy) {
        UbigeoCatalog.UbigeoDistrict district = ubigeoCatalog.findDistrict(academy.getUbigeoCode()).orElse(null);
        return new AcademyResponse(
                academy.getId(),
                academy.getCode(),
                academy.getName(),
                academy.getLogoUrl(),
                academy.getType(),
                academy.getDescription(),
                academy.getFoundationYear(),
                academy.getRuc(),
                academy.getUbigeoCode(),
                district == null ? null : district.departmentName(),
                district == null ? null : district.provinceName(),
                district == null ? null : district.districtName(),
                academy.getAddress(),
                academy.getInstitutionalEmail(),
                academy.getInstitutionalPhone(),
                academy.getWebsiteUrl(),
                academy.getInstagramUrl(),
                academy.getFacebookUrl(),
                academy.getYoutubeUrl(),
                academy.getTiktokUrl(),
                List.copyOf(academy.getTrainingGroups()),
                academy.getHeadquarters(),
                representativeName(academy),
                academy.getOperationalStatus(),
                academy.getVerificationStatus(),
                OrganizationCountsResponse.unavailable(),
                academy.getCreatedAt(),
                academy.getUpdatedAt(),
                academy.getVersion()
        );
    }

    public AcademyDirectoryResponse toAcademyDirectoryResponse(Academy academy) {
        UbigeoCatalog.UbigeoDistrict district = ubigeoCatalog.findDistrict(academy.getUbigeoCode()).orElse(null);
        return new AcademyDirectoryResponse(
                academy.getId(),
                academy.getCode(),
                academy.getName(),
                academy.getLogoUrl(),
                academy.getType(),
                academy.getDescription(),
                academy.getFoundationYear(),
                academy.getUbigeoCode(),
                district == null ? null : district.departmentName(),
                district == null ? null : district.provinceName(),
                district == null ? null : district.districtName(),
                academy.getAddress(),
                academy.getInstitutionalEmail(),
                academy.getInstitutionalPhone(),
                academy.getWebsiteUrl(),
                academy.getInstagramUrl(),
                academy.getFacebookUrl(),
                academy.getYoutubeUrl(),
                academy.getTiktokUrl(),
                List.copyOf(academy.getTrainingGroups()),
                academy.getHeadquarters(),
                OrganizationCountsResponse.unavailable()
        );
    }

    public ClubResponse toClubResponse(Club club) {
        UbigeoCatalog.UbigeoDistrict district = ubigeoCatalog.findDistrict(club.getUbigeoCode()).orElse(null);
        return new ClubResponse(
                club.getId(),
                club.getCode(),
                club.getName(),
                club.getType(),
                club.getRuc(),
                club.getFoundationYear(),
                club.getLogoUrl(),
                club.getDescription(),
                club.getCountryCode(),
                club.getUbigeoCode(),
                district == null ? null : district.departmentName(),
                district == null ? null : district.provinceName(),
                district == null ? null : district.districtName(),
                club.getAddress(),
                club.getInstitutionalEmail(),
                club.getInstitutionalPhone(),
                club.getWebsiteUrl(),
                club.getInstagramUrl(),
                club.getFacebookUrl(),
                club.getYoutubeUrl(),
                club.getTiktokUrl(),
                responsibleNames(club),
                club.getOperationalStatus(),
                club.getVerificationStatus(),
                OrganizationCountsResponse.unavailable(),
                club.getCreatedAt(),
                club.getUpdatedAt(),
                club.getVersion()
        );
    }

    public ClubDirectoryResponse toClubDirectoryResponse(Club club) {
        UbigeoCatalog.UbigeoDistrict district = ubigeoCatalog.findDistrict(club.getUbigeoCode()).orElse(null);
        return new ClubDirectoryResponse(
                club.getId(),
                club.getCode(),
                club.getName(),
                club.getType(),
                club.getFoundationYear(),
                club.getLogoUrl(),
                club.getDescription(),
                club.getCountryCode(),
                club.getUbigeoCode(),
                district == null ? null : district.departmentName(),
                district == null ? null : district.provinceName(),
                district == null ? null : district.districtName(),
                club.getAddress(),
                club.getInstitutionalEmail(),
                club.getInstitutionalPhone(),
                club.getWebsiteUrl(),
                club.getInstagramUrl(),
                club.getFacebookUrl(),
                club.getYoutubeUrl(),
                club.getTiktokUrl(),
                OrganizationCountsResponse.unavailable()
        );
    }

    private List<String> responsibleNames(Club club) {
        return userOrganizationRepository
                .findByClub_Id(club.getId())
                .stream()
                .map(UserOrganization::getUser)
                .filter(Objects::nonNull)
                .map(user -> (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim())
                .filter(name -> !name.isBlank())
                .sorted()
                .toList();
    }

    /**
     * El representante de una academia se deriva de su Director de academia
     * activo (rol ACADEMY_DIRECTOR y cuenta ACTIVE). Si no existe, se conserva
     * el valor legacy almacenado como respaldo; nunca se devuelve cadena vacia.
     */
    private String representativeName(Academy academy) {
        String derived = userOrganizationRepository
                .findByAcademy_Id(academy.getId())
                .stream()
                .filter(association -> association.getUser() != null)
                .filter(association -> association.getUser().getRole() == Role.ACADEMY_DIRECTOR)
                .filter(association -> association.getUser().getStatus() == UserStatus.ACTIVE)
                .sorted(Comparator.comparing(association -> association.getUser().getId()))
                .map(association -> {
                    String firstName = safe(association.getUser().getFirstName());
                    String lastName = safe(association.getUser().getLastName());
                    return (firstName + " " + lastName).trim();
                })
                .filter(name -> !name.isBlank())
                .findFirst()
                .orElse(null);
        if (derived != null) {
            return derived;
        }
        return blankToNull(academy.getRepresentativeName());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
