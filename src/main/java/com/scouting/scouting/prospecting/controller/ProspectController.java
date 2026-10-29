package com.scouting.scouting.prospecting.controller;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.prospecting.dto.ProspectCreateRequest;
import com.scouting.scouting.prospecting.dto.ProspectEvaluationCreateRequest;
import com.scouting.scouting.prospecting.dto.ProspectEvaluationResponse;
import com.scouting.scouting.prospecting.dto.ProspectNotesUpdateRequest;
import com.scouting.scouting.prospecting.dto.ProspectResponse;
import com.scouting.scouting.prospecting.dto.ProspectStatusUpdateRequest;
import com.scouting.scouting.prospecting.entity.ProspectStatus;
import com.scouting.scouting.prospecting.service.ProspectService;
import com.scouting.scouting.security.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API privada de prospecciones; solo el ojeador autor y rol SCOUT. */
@RestController
@RequestMapping("/api/v1/prospects")
@PreAuthorize("hasRole('SCOUT')")
@Validated
public class ProspectController {

    private final ProspectService prospectService;

    public ProspectController(ProspectService prospectService) {
        this.prospectService = prospectService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProspectResponse>> list(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size,
            @RequestParam(defaultValue = "lastActivityAt,desc") String sort,
            @RequestParam(required = false) ProspectStatus status,
            @RequestParam(required = false) @Positive(message = "El jugador no es valido.") Long playerId,
            @RequestParam(required = false) @Size(max = 100, message = "La busqueda no debe superar 100 caracteres.") String q) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(prospectService.list(actor, page, size, sort, status, playerId, q));
    }

    @PostMapping
    public ResponseEntity<ProspectResponse> create(@AuthenticationPrincipal AppUserPrincipal actor,
                                                   @Valid @RequestBody ProspectCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(prospectService.create(actor, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProspectResponse> get(@AuthenticationPrincipal AppUserPrincipal actor,
                                                @PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(prospectService.get(actor, id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProspectResponse> updateNotes(@AuthenticationPrincipal AppUserPrincipal actor,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody ProspectNotesUpdateRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(prospectService.updateNotes(actor, id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProspectResponse> updateStatus(@AuthenticationPrincipal AppUserPrincipal actor,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody ProspectStatusUpdateRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(prospectService.updateStatus(actor, id, request));
    }

    @GetMapping("/{id}/evaluations")
    public ResponseEntity<PageResponse<ProspectEvaluationResponse>> listEvaluations(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(prospectService.listEvaluations(actor, id, page, size));
    }

    @PostMapping("/{id}/evaluations")
    public ResponseEntity<ProspectEvaluationResponse> createEvaluation(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @PathVariable Long id,
            @Valid @RequestBody ProspectEvaluationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(prospectService.createEvaluation(actor, id, request));
    }

    @GetMapping("/{id}/evaluations/{evaluationId}")
    public ResponseEntity<ProspectEvaluationResponse> getEvaluation(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @PathVariable Long id,
            @PathVariable Long evaluationId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(prospectService.getEvaluation(actor, id, evaluationId));
    }
}
