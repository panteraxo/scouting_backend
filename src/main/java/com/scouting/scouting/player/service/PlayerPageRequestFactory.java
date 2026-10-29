package com.scouting.scouting.player.service;

import com.scouting.scouting.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

/** Paginacion y ordenamiento con lista blanca de propiedades de jugador. */
public final class PlayerPageRequestFactory {

    public static final int MAX_PAGE_SIZE = 100;

    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "lastName", "user.lastName",
            "firstName", "user.firstName",
            "birthDate", "birthDate",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private PlayerPageRequestFactory() {
    }

    public static Pageable create(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String requestedSort = sort == null || sort.isBlank() ? "lastName,asc" : sort.trim();

        String[] parts = requestedSort.split(",", -1);
        if (parts.length < 1 || parts.length > 2) {
            throw ApiException.badRequest("INVALID_SORT", "El orden solicitado no es valido.");
        }
        String property = parts[0].trim();
        String entityProperty = SORT_PROPERTIES.get(property);
        if (entityProperty == null) {
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

        return PageRequest.of(safePage, safeSize, Sort.by(direction, entityProperty));
    }
}
