package com.scouting.scouting.admin.service;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.admin.dto.ScoutRegistrationResponse;
import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revision de solicitudes de ojeador. Solo accesible para ADMIN; la autorizacion
 * se valida en la cadena de seguridad y con {@code @PreAuthorize}.
 */
@Service
public class ScoutRegistrationAdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;

    public ScoutRegistrationAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ScoutRegistrationResponse> list(UserStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<User> result = status == null
                ? userRepository.findByRole(Role.SCOUT, pageable)
                : userRepository.findByRoleAndStatus(Role.SCOUT, status, pageable);
        return PageResponse.from(result.map(ScoutRegistrationResponse::from));
    }

    @Transactional
    public ScoutRegistrationResponse approve(Long id) {
        User scout = findPendingScout(id);
        scout.setStatus(UserStatus.ACTIVE);
        return ScoutRegistrationResponse.from(scout);
    }

    @Transactional
    public ScoutRegistrationResponse reject(Long id) {
        User scout = findPendingScout(id);
        scout.setStatus(UserStatus.INACTIVE);
        return ScoutRegistrationResponse.from(scout);
    }

    private User findPendingScout(Long id) {
        // Bloqueo pesimista: la segunda transaccion espera y, al leer el estado
        // ya actualizado, responde 409 en lugar de procesar dos veces.
        User user = userRepository.findByIdForUpdate(id)
                .filter(candidate -> candidate.getRole() == Role.SCOUT)
                .orElseThrow(() -> ApiException.notFound("SCOUT_REGISTRATION_NOT_FOUND", "Solicitud no encontrada."));
        if (user.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw ApiException.conflict("SCOUT_REGISTRATION_ALREADY_PROCESSED", "La solicitud ya fue procesada.");
        }
        return user;
    }
}
