package com.scouting.scouting.player.controller;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.player.dto.PlayerHistoryEntryCorrectionRequest;
import com.scouting.scouting.player.dto.PlayerHistoryEntryCreateRequest;
import com.scouting.scouting.player.dto.PlayerHistoryEntryResponse;
import com.scouting.scouting.player.dto.PlayerHistoryRevisionResponse;
import com.scouting.scouting.player.entity.ActivityType;
import com.scouting.scouting.player.entity.AgeCategory;
import com.scouting.scouting.player.entity.CompetitiveLevel;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.service.PlayerHistoryService;
import com.scouting.scouting.security.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Historial deportivo: POST append-only, PUT correccion auditada, sin DELETE. */
@RestController
@RequestMapping("/api/v1/players/{playerId}/history")
@Validated
public class PlayerHistoryController {

    private final PlayerHistoryService historyService;

    public PlayerHistoryController(PlayerHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR','SCOUT','PLAYER')")
    public ResponseEntity<PageResponse<PlayerHistoryEntryResponse>> list(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size,
            @RequestParam(defaultValue = "activityDate,desc") String sort,
            @RequestParam(required = false) ActivityType activityType,
            @RequestParam(required = false) CompetitiveLevel competitiveLevel,
            @RequestParam(required = false) AgeCategory ageCategory,
            @RequestParam(required = false) PlayerPosition position,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(historyService.list(actor, playerId, page, size, sort, activityType, competitiveLevel,
                        ageCategory, position, year, from, to));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR')")
    public ResponseEntity<PlayerHistoryEntryResponse> create(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @PathVariable Long playerId,
            @Valid @RequestBody PlayerHistoryEntryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(historyService.create(actor, playerId, request));
    }

    @GetMapping("/{historyId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR','SCOUT','PLAYER')")
    public ResponseEntity<PlayerHistoryEntryResponse> get(@AuthenticationPrincipal AppUserPrincipal actor,
                                                           @PathVariable Long playerId,
                                                           @PathVariable Long historyId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(historyService.get(actor, playerId, historyId));
    }

    @PutMapping("/{historyId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR')")
    public ResponseEntity<PlayerHistoryEntryResponse> correct(@AuthenticationPrincipal AppUserPrincipal actor,
                                                              @PathVariable Long playerId,
                                                              @PathVariable Long historyId,
                                                              @Valid @RequestBody PlayerHistoryEntryCorrectionRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(historyService.correct(actor, playerId, historyId, request));
    }

    @GetMapping("/{historyId}/revisions")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR')")
    public ResponseEntity<PageResponse<PlayerHistoryRevisionResponse>> revisions(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @PathVariable Long playerId,
            @PathVariable Long historyId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(historyService.revisions(actor, playerId, historyId, page, size));
    }
}
