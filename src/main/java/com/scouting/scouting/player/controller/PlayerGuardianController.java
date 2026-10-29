package com.scouting.scouting.player.controller;

import com.scouting.scouting.player.dto.GuardianConsentUpdateRequest;
import com.scouting.scouting.player.dto.PlayerGuardianUpsertRequest;
import com.scouting.scouting.player.service.PlayerGuardianService;
import com.scouting.scouting.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint aislado para datos del tutor; nunca se incluyen en el perfil general. */
@RestController
@RequestMapping("/api/v1/players/{playerId}/guardian")
@Validated
public class PlayerGuardianController {

    private final PlayerGuardianService guardianService;

    public PlayerGuardianController(PlayerGuardianService guardianService) {
        this.guardianService = guardianService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR','SCOUT','PLAYER')")
    public ResponseEntity<Object> get(@AuthenticationPrincipal AppUserPrincipal actor,
                                      @PathVariable Long playerId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(guardianService.get(actor, playerId));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR','SCOUT','PLAYER')")
    public ResponseEntity<Object> upsert(@AuthenticationPrincipal AppUserPrincipal actor,
                                         @PathVariable Long playerId,
                                         @Valid @RequestBody PlayerGuardianUpsertRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(guardianService.upsert(actor, playerId, request));
    }

    @PutMapping("/consent")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR','SCOUT','PLAYER')")
    public ResponseEntity<Object> consent(@AuthenticationPrincipal AppUserPrincipal actor,
                                          @PathVariable Long playerId,
                                          @Valid @RequestBody GuardianConsentUpdateRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(guardianService.updateConsent(actor, playerId, request));
    }
}
