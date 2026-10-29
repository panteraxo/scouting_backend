package com.scouting.scouting.prospecting.entity;

/**
 * Estado de una prospeccion. No existe borrado; cerrar conserva evaluaciones.
 */
public enum ProspectStatus {
    FOLLOWING,
    PAUSED,
    CLOSED;

    public boolean canTransitionTo(ProspectStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case FOLLOWING -> target == PAUSED || target == CLOSED;
            case PAUSED -> target == FOLLOWING || target == CLOSED;
            case CLOSED -> target == FOLLOWING;
        };
    }
}
