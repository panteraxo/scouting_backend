package com.scouting.scouting.admin.controller;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.admin.dto.ScoutRegistrationResponse;
import com.scouting.scouting.admin.service.ScoutRegistrationAdminService;
import com.scouting.scouting.user.entity.UserStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Revision de solicitudes de ojeador. Solo ADMIN autenticado; cualquier otro
 * rol recibe 403 aunque conozca la URL.
 */
@RestController
@RequestMapping("/api/v1/admin/scout-registrations")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class ScoutRegistrationAdminController {

    private final ScoutRegistrationAdminService adminService;

    public ScoutRegistrationAdminController(ScoutRegistrationAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public PageResponse<ScoutRegistrationResponse> list(
            @RequestParam(defaultValue = "PENDING_APPROVAL") UserStatus status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size) {
        return adminService.list(status, page, size);
    }

    @PostMapping("/{id}/approve")
    public ScoutRegistrationResponse approve(@PathVariable Long id) {
        return adminService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public ScoutRegistrationResponse reject(@PathVariable Long id) {
        return adminService.reject(id);
    }
}
