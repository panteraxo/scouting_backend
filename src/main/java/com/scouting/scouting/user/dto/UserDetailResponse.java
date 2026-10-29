package com.scouting.scouting.user.dto;

import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.user.entity.DocumentType;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;

import java.time.Instant;

/**
 * Detalle administrativo de una cuenta. Incluye documento y telefono (solo para
 * ADMIN), pero nunca hash/contrasena temporal, datos de tutor ni notas privadas.
 */
public record UserDetailResponse(
        Long id,
        String firstName,
        String lastName,
        DocumentType documentType,
        String documentNumber,
        String email,
        String phone,
        String username,
        Role role,
        UserStatus status,
        boolean mustChangePassword,
        Instant temporaryPasswordExpiresAt,
        Long academyId,
        String academyCode,
        String academyName,
        Long clubId,
        String clubCode,
        String clubName,
        String position,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserDetailResponse from(User user, UserOrganization organization) {
        Academy academy = organization == null ? null : organization.getAcademy();
        Club club = organization == null ? null : organization.getClub();
        return new UserDetailResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getDocumentType(),
                user.getDocumentNumber(),
                user.getEmail(),
                user.getPhone(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                user.isMustChangePassword(),
                user.getTemporaryPasswordExpiresAt(),
                academy == null ? null : academy.getId(),
                academy == null ? null : academy.getCode(),
                academy == null ? null : academy.getName(),
                club == null ? null : club.getId(),
                club == null ? null : club.getCode(),
                club == null ? null : club.getName(),
                organization == null ? null : organization.getPosition(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
