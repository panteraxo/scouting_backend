package com.scouting.scouting.user.entity;

/**
 * Estado de una cuenta.
 *
 * <ul>
 *   <li>{@link #ACTIVE}: cuenta habilitada; puede iniciar sesion plena.</li>
 *   <li>{@link #PENDING_APPROVAL}: registro publico de ojeador pendiente de aprobacion; sin sesion plena.</li>
 *   <li>{@link #INACTIVE}: cuenta rechazada o deshabilitada; sin acceso.</li>
 * </ul>
 */
public enum UserStatus {
    ACTIVE,
    PENDING_APPROVAL,
    INACTIVE
}
