package com.scouting.scouting.organization.dto;

/**
 * Conteos derivados de relaciones reales. En SCP-003 las fuentes de jugadores,
 * aspirantes, profesores y equipos aun no existen, por lo que todos los valores
 * se exponen como null. Nunca se devuelve cero por ausencia de fuente.
 */
public record OrganizationCountsResponse(
        Long players,
        Long prospects,
        Long professors,
        Long teams
) {

    public static OrganizationCountsResponse unavailable() {
        return new OrganizationCountsResponse(null, null, null, null);
    }
}
