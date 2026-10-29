package com.scouting.scouting.organization.dto;

import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.VerificationStatus;

import java.time.Instant;
import java.util.List;

/**
 * Representacion completa de club para ADMIN y para el propio Responsable.
 * Los nombres de responsables se derivan de UserOrganization; no se exponen
 * DNI, correo ni telefono personales.
 */
public record ClubResponse(
        Long id,
        String code,
        String name,
        String type,
        String ruc,
        Integer foundationYear,
        String logoUrl,
        String description,
        String countryCode,
        String ubigeoCode,
        String departmentName,
        String provinceName,
        String districtName,
        String address,
        String institutionalEmail,
        String institutionalPhone,
        String websiteUrl,
        String instagramUrl,
        String facebookUrl,
        String youtubeUrl,
        String tiktokUrl,
        List<String> responsibleNames,
        OperationalStatus operationalStatus,
        VerificationStatus verificationStatus,
        OrganizationCountsResponse counts,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
