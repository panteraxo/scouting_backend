package com.scouting.scouting.organization.controller;

import com.scouting.scouting.organization.dto.AcademyResponse;
import com.scouting.scouting.organization.dto.AcademyUpdateRequest;
import com.scouting.scouting.organization.service.AcademyService;
import com.scouting.scouting.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Academia de la cuenta autenticada. No recibe id de organizacion: el alcance
 * se resuelve en el servidor desde UserOrganization.
 */
@RestController
@RequestMapping("/api/v1/academies/me")
public class AcademyScopeController {

    private final AcademyService academyService;

    public AcademyScopeController(AcademyService academyService) {
        this.academyService = academyService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ACADEMY_DIRECTOR','PROFESSOR')")
    public AcademyResponse getOwn(@AuthenticationPrincipal AppUserPrincipal actor) {
        return academyService.getOwn(actor);
    }

    @PutMapping
    @PreAuthorize("hasRole('ACADEMY_DIRECTOR')")
    public AcademyResponse updateOwn(@AuthenticationPrincipal AppUserPrincipal actor,
                                     @Valid @RequestBody AcademyUpdateRequest request) {
        return academyService.updateOwn(actor, request);
    }

    @PatchMapping("/deactivate")
    @PreAuthorize("hasRole('ACADEMY_DIRECTOR')")
    public AcademyResponse deactivateOwn(@AuthenticationPrincipal AppUserPrincipal actor) {
        return academyService.deactivateOwn(actor);
    }
}
