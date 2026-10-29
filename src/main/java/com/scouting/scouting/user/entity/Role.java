package com.scouting.scouting.user.entity;

/**
 * Roles iniciales del sistema. Cada cuenta tiene un unico rol.
 *
 * <p>En SCP-002 solo se usan {@link #ADMIN} y {@link #SCOUT}. Los demas roles se
 * mantienen definidos para que las fases de organizaciones y usuarios (SCP-003/SCP-004)
 * puedan asignarlos sin reescribir el modelo.</p>
 */
public enum Role {
    ADMIN,
    PROFESSOR,
    ACADEMY_DIRECTOR,
    SCOUT,
    CLUB_MANAGER,
    PLAYER
}
