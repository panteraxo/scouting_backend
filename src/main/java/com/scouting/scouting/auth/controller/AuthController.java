package com.scouting.scouting.auth.controller;

import com.scouting.scouting.auth.dto.ChangePasswordRequest;
import com.scouting.scouting.auth.dto.CsrfResponse;
import com.scouting.scouting.auth.dto.LoginRequest;
import com.scouting.scouting.auth.dto.RegisterScoutRequest;
import com.scouting.scouting.auth.dto.ScoutRegistrationResponse;
import com.scouting.scouting.auth.dto.UserResponse;
import com.scouting.scouting.auth.service.AuthService;
import com.scouting.scouting.auth.service.ScoutRegistrationService;
import com.scouting.scouting.security.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticacion. La identidad se obtiene del contexto de seguridad
 * del servidor; ningun endpoint acepta usuario, rol ni organizacion desde el request.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String GENERIC_REGISTRATION_MESSAGE =
            "Solicitud registrada. Un administrador revisara la cuenta para habilitarla.";

    private final AuthService authService;
    private final ScoutRegistrationService scoutRegistrationService;

    public AuthController(AuthService authService, ScoutRegistrationService scoutRegistrationService) {
        this.authService = authService;
        this.scoutRegistrationService = scoutRegistrationService;
    }

    /** Devuelve el token CSRF para las operaciones de escritura. */
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName(), csrfToken.getParameterName());
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        return ResponseEntity.ok(authService.login(request, httpRequest, httpResponse));
    }

    /** Registro publico exclusivo de ojeadores. No crea sesion. */
    @PostMapping("/register/scout")
    public ResponseEntity<ScoutRegistrationResponse> registerScout(@Valid @RequestBody RegisterScoutRequest request) {
        scoutRegistrationService.register(request);
        return ResponseEntity.accepted().body(new ScoutRegistrationResponse(GENERIC_REGISTRATION_MESSAGE));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return authService.me(principal);
    }

    @PostMapping("/change-password")
    public UserResponse changePassword(@AuthenticationPrincipal AppUserPrincipal principal,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(principal, request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
        return ResponseEntity.noContent().build();
    }
}
