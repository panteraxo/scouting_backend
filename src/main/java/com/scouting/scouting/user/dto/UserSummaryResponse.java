package com.scouting.scouting.user.dto;

import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;

/**
 * Vista minima de cuenta para listados. No incluye documento, telefono, hash de
 * contrasena, contrasena temporal, datos de tutor ni notas privadas.
 */
public record UserSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String username,
        String email,
        Role role,
        UserStatus status,
        Long academyId,
        String academyCode,
        String academyName,
        Long clubId,
        String clubCode,
        String clubName
) {

    public static UserSummaryResponse from(User user, UserOrganization organization) {
        Academy academy = organization == null ? null : organization.getAcademy();
        Club club = organization == null ? null : organization.getClub();
        return new UserSummaryResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                academy == null ? null : academy.getId(),
                academy == null ? null : academy.getCode(),
                academy == null ? null : academy.getName(),
                club == null ? null : club.getId(),
                club == null ? null : club.getCode(),
                club == null ? null : club.getName()
        );
    }
}
