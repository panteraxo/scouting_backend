package com.scouting.scouting.player.entity;

/**
 * Catalogo cerrado de metricas posicionales. No se aceptan codigos fuera de
 * esta lista.
 */
public enum PlayerMetricCode {
    SAVES(PositionGroup.GOALKEEPER, "saves"),
    GOALS_CONCEDED(PositionGroup.GOALKEEPER, "goalsConceded"),
    CLEAN_SHEET(PositionGroup.GOALKEEPER, "cleanSheet"),
    CROSS_INTERVENTIONS(PositionGroup.GOALKEEPER, "crossInterventions"),
    GK_PASSES_ATTEMPTED(PositionGroup.GOALKEEPER, "passesAttempted"),
    GK_PASSES_COMPLETED(PositionGroup.GOALKEEPER, "passesCompleted"),

    SUCCESSFUL_TACKLES(PositionGroup.DEFENDER, "successfulTackles"),
    DEF_INTERCEPTIONS(PositionGroup.DEFENDER, "interceptions"),
    CLEARANCES(PositionGroup.DEFENDER, "clearances"),
    DEF_DUELS_WON(PositionGroup.DEFENDER, "duelsWon"),
    BLOCKS(PositionGroup.DEFENDER, "blocks"),
    ERRORS_LEADING_TO_CHANCE(PositionGroup.DEFENDER, "errorsLeadingToChance"),

    MID_PASSES_ATTEMPTED(PositionGroup.MIDFIELDER, "passesAttempted"),
    MID_PASSES_COMPLETED(PositionGroup.MIDFIELDER, "passesCompleted"),
    MID_INTERCEPTIONS(PositionGroup.MIDFIELDER, "interceptions"),
    KEY_PASSES(PositionGroup.MIDFIELDER, "keyPasses"),
    MID_ASSISTS(PositionGroup.MIDFIELDER, "assists"),
    RECOVERIES(PositionGroup.MIDFIELDER, "recoveries"),
    MID_DUELS_WON(PositionGroup.MIDFIELDER, "duelsWon"),
    MID_SUCCESSFUL_DRIBBLES(PositionGroup.MIDFIELDER, "successfulDribbles"),
    MID_SHOTS(PositionGroup.MIDFIELDER, "shots"),

    FWD_GOALS(PositionGroup.FORWARD, "goals"),
    FWD_ASSISTS(PositionGroup.FORWARD, "assists"),
    FWD_SHOTS(PositionGroup.FORWARD, "shots"),
    FWD_SUCCESSFUL_DRIBBLES(PositionGroup.FORWARD, "successfulDribbles"),
    CHANCES_CREATED(PositionGroup.FORWARD, "chancesCreated"),
    CROSSES_ATTEMPTED(PositionGroup.FORWARD, "crossesAttempted"),
    CROSSES_COMPLETED(PositionGroup.FORWARD, "crossesCompleted");

    private final PositionGroup group;
    private final String code;

    PlayerMetricCode(PositionGroup group, String code) {
        this.group = group;
        this.code = code;
    }

    public PositionGroup group() {
        return group;
    }

    public String code() {
        return code;
    }
}
