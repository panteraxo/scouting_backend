package com.scouting.scouting.organization.dto;

import java.util.List;

/**
 * Vista de academia para el directorio de ojeadores. Omite RUC, nombre del
 * representante, estados internos y cualquier dato personal.
 */
public record AcademyDirectoryResponse(
        Long id,
        String code,
        String name,
        String logoUrl,
        String type,
        String description,
        Integer foundationYear,
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
        OrganizationCountsResponse counts
) {
}
