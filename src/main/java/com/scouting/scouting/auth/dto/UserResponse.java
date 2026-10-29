package com.scouting.scouting.auth.dto;

import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;

/**
 * Respuesta publica de cuenta. Nunca incluye hash de contrasena, datos de tutor
 * ni notas privadas de ojeadores.
 */
public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String username,
        Role role,
        UserStatus status,
        boolean mustChangePassword
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                user.isMustChangePassword()
        );
    }
}
