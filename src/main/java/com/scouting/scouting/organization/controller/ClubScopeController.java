package com.scouting.scouting.organization.controller;

import com.scouting.scouting.organization.dto.ClubResponse;
import com.scouting.scouting.organization.dto.ClubUpdateRequest;
import com.scouting.scouting.organization.service.ClubService;
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
 * Club de la cuenta autenticada. No recibe id de organizacion: el alcance se
 * resuelve en el servidor desde UserOrganization.
 */
@RestController
@RequestMapping("/api/v1/clubs/me")
public class ClubScopeController {

    private final ClubService clubService;

    public ClubScopeController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CLUB_MANAGER')")
    public ClubResponse getOwn(@AuthenticationPrincipal AppUserPrincipal actor) {
        return clubService.getOwn(actor);
    }

    @PutMapping
    @PreAuthorize("hasRole('CLUB_MANAGER')")
    public ClubResponse updateOwn(@AuthenticationPrincipal AppUserPrincipal actor,
                                  @Valid @RequestBody ClubUpdateRequest request) {
        return clubService.updateOwn(actor, request);
    }

    @PatchMapping("/deactivate")
    @PreAuthorize("hasRole('CLUB_MANAGER')")
    public ClubResponse deactivateOwn(@AuthenticationPrincipal AppUserPrincipal actor) {
        return clubService.deactivateOwn(actor);
    }
}
