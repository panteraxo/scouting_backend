package com.scouting.scouting.organization.dto;

import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.VerificationStatus;

import java.time.Instant;
import java.util.List;

/**
 * Representacion completa de academia para ADMIN y para el propio Director o
 * Profesor. Incluye RUC y nombre del representante; nunca se usa en el
 * directorio de ojeadores.
 */
public record AcademyResponse(
        Long id,
        String code,
        String name,
        String logoUrl,
        String type,
        String description,
        Integer foundationYear,
        String ruc,
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
        List<String> trainingGroups,
        String headquarters,
        String representativeName,
        OperationalStatus operationalStatus,
        VerificationStatus verificationStatus,
        OrganizationCountsResponse counts,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
