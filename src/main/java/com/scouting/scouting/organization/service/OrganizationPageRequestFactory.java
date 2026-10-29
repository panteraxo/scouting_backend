package com.scouting.scouting.organization.service;

import com.scouting.scouting.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/** Paginacion y ordenamiento con lista blanca de propiedades. */
public final class OrganizationPageRequestFactory {

    public static final int MAX_PAGE_SIZE = 100;

    public static final Set<String> ACADEMY_SORT_PROPERTIES = Set.of(
            "name", "code", "type", "foundationYear", "createdAt", "updatedAt",
            "operationalStatus", "verificationStatus");
    public static final Set<String> CLUB_SORT_PROPERTIES = Set.of(
            "name", "code", "type", "foundationYear", "createdAt", "updatedAt",
            "operationalStatus", "verificationStatus");

    private OrganizationPageRequestFactory() {
    }

    public static Pageable create(int page, int size, String sort, String defaultSort,
                                  Set<String> allowedProperties) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String requestedSort = sort == null || sort.isBlank() ? defaultSort : sort.trim();

        String[] parts = requestedSort.split(",", -1);
        if (parts.length < 1 || parts.length > 2) {
            throw ApiException.badRequest("INVALID_SORT", "El orden solicitado no es valido.");
        }
        String property = parts[0].trim();
        if (!allowedProperties.contains(property)) {
            throw ApiException.badRequest("INVALID_SORT", "El orden solicitado no es valido.");
        }

        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length == 2) {
            String rawDirection = parts[1].trim();
            if (rawDirection.isEmpty()) {
                throw ApiException.badRequest("INVALID_SORT", "El orden solicitado no es valido.");
            }
            try {
                direction = Sort.Direction.fromString(rawDirection);
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("INVALID_SORT", "El orden solicitado no es valido.");
            }
        }

        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }
}
