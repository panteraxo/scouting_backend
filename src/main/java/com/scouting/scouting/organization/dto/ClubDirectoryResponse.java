package com.scouting.scouting.organization.dto;

/**
 * Vista de club para el directorio de ojeadores. Omite RUC, responsables,
 * estados internos y cualquier dato personal.
 */
public record ClubDirectoryResponse(
        Long id,
        String code,
        String name,
        String type,
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
        OrganizationCountsResponse counts
) {
}
