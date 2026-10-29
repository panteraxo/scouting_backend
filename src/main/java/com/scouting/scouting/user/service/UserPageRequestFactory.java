package com.scouting.scouting.user.service;

import com.scouting.scouting.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/** Paginacion y ordenamiento de usuarios con lista blanca de propiedades. */
public final class UserPageRequestFactory {

    public static final int MAX_PAGE_SIZE = 100;

    public static final Set<String> SORT_PROPERTIES = Set.of(
            "id", "firstName", "lastName", "username", "email", "role", "status",
            "createdAt", "updatedAt");

    private UserPageRequestFactory() {
    }

    public static Pageable create(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String requestedSort = sort == null || sort.isBlank() ? "createdAt,desc" : sort.trim();

        String[] parts = requestedSort.split(",", -1);
        if (parts.length < 1 || parts.length > 2) {
            throw ApiException.badRequest("INVALID_SORT", "El orden solicitado no es valido.");
        }
        String property = parts[0].trim();
        if (!SORT_PROPERTIES.contains(property)) {
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
