package com.scouting.scouting.player.entity;

/**
 * Catalogo cerrado de posiciones detalladas. El grupo se deriva siempre en el
 * servidor; el cliente nunca puede enviarlo como autoridad.
 */
public enum PlayerPosition {
    GOALKEEPER(PositionGroup.GOALKEEPER),
    CENTER_BACK(PositionGroup.DEFENDER),
    FULL_BACK(PositionGroup.DEFENDER),
    WING_BACK(PositionGroup.DEFENDER),
    DEFENSIVE_MIDFIELDER(PositionGroup.MIDFIELDER),
    CENTRAL_MIDFIELDER(PositionGroup.MIDFIELDER),
    ATTACKING_MIDFIELDER(PositionGroup.MIDFIELDER),
    LEFT_MIDFIELDER(PositionGroup.MIDFIELDER),
    RIGHT_MIDFIELDER(PositionGroup.MIDFIELDER),
    LEFT_WINGER(PositionGroup.FORWARD),
    RIGHT_WINGER(PositionGroup.FORWARD),
    SECOND_STRIKER(PositionGroup.FORWARD),
    STRIKER(PositionGroup.FORWARD);

    private final PositionGroup group;

    PlayerPosition(PositionGroup group) {
        this.group = group;
    }

    public PositionGroup group() {
        return group;
    }
}
