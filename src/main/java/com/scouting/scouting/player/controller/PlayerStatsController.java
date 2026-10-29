package com.scouting.scouting.player.controller;

import com.scouting.scouting.player.dto.PlayerStatsResponse;
import com.scouting.scouting.player.service.PlayerStatsService;
import com.scouting.scouting.security.AppUserPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players/{playerId}/stats")
public class PlayerStatsController {

    private final PlayerStatsService statsService;

    public PlayerStatsController(PlayerStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR','SCOUT','PLAYER')")
    public ResponseEntity<PlayerStatsResponse> stats(@AuthenticationPrincipal AppUserPrincipal actor,
                                                     @PathVariable Long playerId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(statsService.stats(actor, playerId));
    }
}
