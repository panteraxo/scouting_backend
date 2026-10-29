package com.scouting.scouting.user.controller;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.dto.AdminUserCreateRequest;
import com.scouting.scouting.user.dto.UserAssignmentUpdateRequest;
import com.scouting.scouting.user.dto.UserCreatedResponse;
import com.scouting.scouting.user.dto.UserDetailResponse;
import com.scouting.scouting.user.dto.UserIdentityUpdateRequest;
import com.scouting.scouting.user.dto.UserStatusUpdateRequest;
import com.scouting.scouting.user.dto.UserSummaryResponse;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.service.UserManagementService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administracion de cuentas exclusiva para ADMIN. Los secretos temporales solo
 * se devuelven en las respuestas de creacion/reseteo y con {@code no-store}.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminUserController {

    private final UserManagementService userManagementService;

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public PageResponse<UserSummaryResponse> list(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) @Size(max = 100, message = "La busqueda no debe superar 100 caracteres.")
            String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) @Positive(message = "La academia no es valida.") Long academyId,
            @RequestParam(required = false) @Positive(message = "El club no es valido.") Long clubId) {
        return userManagementService.listAdmin(actor, page, size, sort, q, role, status, academyId, clubId);
    }

    @GetMapping("/{id}")
    public UserDetailResponse get(@AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        return userManagementService.getAdmin(actor, id);
    }

    @PostMapping
    public ResponseEntity<UserCreatedResponse> create(@AuthenticationPrincipal AppUserPrincipal actor,
                                                      @Valid @RequestBody AdminUserCreateRequest request) {
        return secretResponse(HttpStatus.CREATED, userManagementService.createAdmin(actor, request));
    }

    @PutMapping("/{id}")
    public UserDetailResponse updateIdentity(@AuthenticationPrincipal AppUserPrincipal actor,
                                             @PathVariable Long id,
                                             @Valid @RequestBody UserIdentityUpdateRequest request) {
        return userManagementService.updateIdentity(actor, id, request);
    }

    @PatchMapping("/{id}/assignment")
    public UserDetailResponse updateAssignment(@AuthenticationPrincipal AppUserPrincipal actor,
                                               @PathVariable Long id,
                                               @Valid @RequestBody UserAssignmentUpdateRequest request) {
        return userManagementService.updateAssignment(actor, id, request);
    }

    @PatchMapping("/{id}/status")
    public UserDetailResponse updateStatus(@AuthenticationPrincipal AppUserPrincipal actor,
                                           @PathVariable Long id,
                                           @Valid @RequestBody UserStatusUpdateRequest request) {
        return userManagementService.updateStatus(actor, id, request);
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<UserCreatedResponse> resetPassword(@AuthenticationPrincipal AppUserPrincipal actor,
                                                             @PathVariable Long id) {
        return secretResponse(HttpStatus.OK, userManagementService.resetPassword(actor, id));
    }

    private ResponseEntity<UserCreatedResponse> secretResponse(HttpStatus status, UserCreatedResponse body) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(body);
    }
}
