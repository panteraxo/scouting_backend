package com.scouting.scouting.ubigeo.controller;

import com.scouting.scouting.ubigeo.dto.UbigeoOptionResponse;
import com.scouting.scouting.ubigeo.service.UbigeoCatalog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalogo UBIGEO local. Jugador no tiene acceso; el resto de roles operativos
 * puede consultarlo para formularios y filtros.
 */
@RestController
@RequestMapping("/api/v1/ubigeo")
@PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR','CLUB_MANAGER','SCOUT')")
public class UbigeoController {

    private final UbigeoCatalog ubigeoCatalog;

    public UbigeoController(UbigeoCatalog ubigeoCatalog) {
        this.ubigeoCatalog = ubigeoCatalog;
    }

    @GetMapping("/departments")
    public List<UbigeoOptionResponse> departments() {
        return ubigeoCatalog.departments();
    }

    @GetMapping("/provinces")
    public List<UbigeoOptionResponse> provinces(@RequestParam String departmentCode) {
        return ubigeoCatalog.provinces(departmentCode);
    }

    @GetMapping("/districts")
    public List<UbigeoOptionResponse> districts(@RequestParam String provinceCode) {
        return ubigeoCatalog.districts(provinceCode);
    }
}
