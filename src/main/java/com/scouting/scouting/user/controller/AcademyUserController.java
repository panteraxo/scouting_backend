package com.scouting.scouting.user.controller;

import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.dto.AcademyUserCreateRequest;
import com.scouting.scouting.user.dto.UserCreatedResponse;
import com.scouting.scouting.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Creacion de cuentas dentro de la academia de la sesion. No existen listado,
 * detalle ni edicion de usuarios generales para PROFESSOR/ACADEMY_DIRECTOR.
 */
@RestController
@RequestMapping("/api/v1/academies/me/users")
public class AcademyUserController {

    private final UserManagementService userManagementService;

    public AcademyUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ACADEMY_DIRECTOR','PROFESSOR')")
    public ResponseEntity<UserCreatedResponse> create(@AuthenticationPrincipal AppUserPrincipal actor,
                                                      @Valid @RequestBody AcademyUserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(userManagementService.createScoped(actor, request));
    }
}
