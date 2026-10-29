package com.scouting.scouting.player.controller;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.player.dto.PlayerAccountCandidateResponse;
import com.scouting.scouting.player.dto.PlayerAccountCreateRequest;
import com.scouting.scouting.player.dto.PlayerAccountCreatedResponse;
import com.scouting.scouting.player.dto.PlayerCreateRequest;
import com.scouting.scouting.player.dto.PlayerDirectoryItemResponse;
import com.scouting.scouting.player.dto.PlayerStatusUpdateRequest;
import com.scouting.scouting.player.dto.PlayerUpdateRequest;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PlayerProfileStatus;
import com.scouting.scouting.player.entity.PositionGroup;
import com.scouting.scouting.player.service.PlayerAccountService;
import com.scouting.scouting.player.service.PlayerProfileService;
import com.scouting.scouting.security.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Perfiles de jugador. La academia y la identidad salen siempre de la sesion. */
@RestController
@RequestMapping("/api/v1/players")
@Validated
public class PlayerController {

    private final PlayerProfileService playerProfileService;
    private final PlayerAccountService playerAccountService;

    public PlayerController(PlayerProfileService playerProfileService,
                            PlayerAccountService playerAccountService) {
        this.playerProfileService = playerProfileService;
        this.playerAccountService = playerAccountService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR','SCOUT')")
    public ResponseEntity<PageResponse<PlayerDirectoryItemResponse>> list(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size,
            @RequestParam(defaultValue = "lastName,asc") String sort,
            @RequestParam(required = false) @Size(max = 100, message = "La busqueda no debe superar 100 caracteres.") String q,
            @RequestParam(required = false) PlayerPosition position,
            @RequestParam(required = false) PositionGroup positionGroup,
            @RequestParam(required = false) @Min(0) @Max(100) Integer ageMin,
            @RequestParam(required = false) @Min(0) @Max(100) Integer ageMax,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String provinceCode,
            @RequestParam(required = false) String districtCode,
            @RequestParam(required = false) Long academyId,
            @RequestParam(required = false) PlayerOperationalStatus operationalStatus,
            @RequestParam(required = false) PlayerProfileStatus profileStatus) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(playerProfileService.list(actor, page, size, sort, q, position, positionGroup, ageMin,
                        ageMax, regionCode, provinceCode, districtCode, academyId, operationalStatus, profileStatus));
    }

    @GetMapping("/account-candidates")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR')")
    public ResponseEntity<PageResponse<PlayerAccountCandidateResponse>> accountCandidates(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size,
            @RequestParam(defaultValue = "lastName,asc") String sort,
            @RequestParam(required = false) @Size(max = 100, message = "La busqueda no debe superar 100 caracteres.") String q,
            @RequestParam(required = false) Long academyId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(playerProfileService.accountCandidates(actor, page, size, sort, q, academyId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR')")
    public ResponseEntity<Object> create(@AuthenticationPrincipal AppUserPrincipal actor,
                                         @Valid @RequestBody PlayerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(playerProfileService.create(actor, request));
    }

    /**
     * Alta conjunta de cuenta PLAYER y perfil deportivo. La contrasena temporal
     * se devuelve una sola vez y la respuesta nunca debe almacenarse en cache.
     */
    @PostMapping("/with-account")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR')")
    public ResponseEntity<PlayerAccountCreatedResponse> createWithAccount(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @Valid @RequestBody PlayerAccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(playerAccountService.create(actor, request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<Object> me(@AuthenticationPrincipal AppUserPrincipal actor) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(playerProfileService.getMe(actor));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR','SCOUT','PLAYER')")
    public ResponseEntity<Object> get(@AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(playerProfileService.get(actor, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR')")
    public ResponseEntity<Object> update(@AuthenticationPrincipal AppUserPrincipal actor,
                                         @PathVariable Long id,
                                         @Valid @RequestBody PlayerUpdateRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(playerProfileService.update(actor, id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR')")
    public ResponseEntity<Object> updateStatus(@AuthenticationPrincipal AppUserPrincipal actor,
                                               @PathVariable Long id,
                                               @Valid @RequestBody PlayerStatusUpdateRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(playerProfileService.updateStatus(actor, id, request));
    }
}
