package com.scouting.scouting.admin.dto;

import com.scouting.scouting.user.entity.DocumentType;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;

import java.time.Instant;

/**
 * Vista de una solicitud de ojeador para el administrador. No incluye hash de
 * contrasena ni datos de tutor/notas privadas.
 */
public record ScoutRegistrationResponse(
        Long id,
        String firstName,
        String lastName,
        DocumentType documentType,
        String documentNumber,
        String email,
        String phone,
        String username,
        UserStatus status,
        Instant createdAt
) {

    public static ScoutRegistrationResponse from(User user) {
        return new ScoutRegistrationResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getDocumentType(),
                user.getDocumentNumber(),
                user.getEmail(),
                user.getPhone(),
                user.getUsername(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
