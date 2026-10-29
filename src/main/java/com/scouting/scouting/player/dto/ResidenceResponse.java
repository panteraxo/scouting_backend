package com.scouting.scouting.player.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Proyeccion de ubicacion UBIGEO. El distrito se omite en vistas restringidas. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResidenceResponse(
        String departmentCode,
        String departmentName,
        String provinceCode,
        String provinceName,
        String districtCode,
        String districtName
) {
}
