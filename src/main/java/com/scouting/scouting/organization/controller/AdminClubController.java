package com.scouting.scouting.organization.controller;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.organization.dto.ClubCreateRequest;
import com.scouting.scouting.organization.dto.ClubResponse;
import com.scouting.scouting.organization.dto.ClubUpdateRequest;
import com.scouting.scouting.organization.dto.VerificationUpdateRequest;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.VerificationStatus;
import com.scouting.scouting.organization.service.ClubService;
import com.scouting.scouting.security.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administracion de clubes exclusiva para ADMIN. */
@RestController
@RequestMapping("/api/v1/admin/clubs")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminClubController {

    private final ClubService clubService;

    public AdminClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping
    public PageResponse<ClubResponse> list(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) @Size(max = 100, message = "La busqueda no debe superar 100 caracteres.")
            String q,
            @RequestParam(required = false) @Size(max = 100, message = "El tipo no debe superar 100 caracteres.")
            String type,
            @RequestParam(required = false) String ubigeo,
            @RequestParam(required = false) OperationalStatus operationalStatus,
            @RequestParam(required = false) VerificationStatus verificationStatus) {
        return clubService.listAdmin(actor, page, size, sort, q, type, ubigeo,
                operationalStatus, verificationStatus);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClubResponse create(@AuthenticationPrincipal AppUserPrincipal actor,
                               @Valid @RequestBody ClubCreateRequest request) {
        return clubService.create(actor, request);
    }

    @GetMapping("/{id}")
    public ClubResponse get(@AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        return clubService.getAdmin(actor, id);
    }

    @PutMapping("/{id}")
    public ClubResponse update(@AuthenticationPrincipal AppUserPrincipal actor,
                               @PathVariable Long id,
                               @Valid @RequestBody ClubUpdateRequest request) {
        return clubService.updateAdmin(actor, id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public ClubResponse deactivate(@AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        return clubService.deactivateAdmin(actor, id);
    }

    @PatchMapping("/{id}/reactivate")
    public ClubResponse reactivate(@AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        return clubService.reactivateAdmin(actor, id);
    }

    @PatchMapping("/{id}/verification")
    public ClubResponse updateVerification(@AuthenticationPrincipal AppUserPrincipal actor,
                                            @PathVariable Long id,
                                            @Valid @RequestBody VerificationUpdateRequest request) {
        return clubService.updateVerification(actor, id, request);
    }
}
