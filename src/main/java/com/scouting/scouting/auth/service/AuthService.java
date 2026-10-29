package com.scouting.scouting.auth.service;

import com.scouting.scouting.auth.dto.ChangePasswordRequest;
import com.scouting.scouting.auth.dto.LoginRequest;
import com.scouting.scouting.auth.dto.UserResponse;
import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de autenticacion y sesion. La identidad del actor siempre proviene del
 * contexto de seguridad; nunca de parametros enviados por el navegador.
 */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Credenciales invalidas.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository,
                       SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    /**
     * Autentica por correo y crea la sesion. Cuentas pendientes, inactivas o con
     * contrasena temporal expirada reciben el mismo error generico.
     */
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = request.email().trim().toLowerCase();
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException ex) {
            throw ApiException.unauthorized("INVALID_CREDENTIALS", INVALID_CREDENTIALS_MESSAGE);
        }

        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", INVALID_CREDENTIALS_MESSAGE));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(AppUserPrincipal principal) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", INVALID_CREDENTIALS_MESSAGE));
        return UserResponse.from(user);
    }

    /**
     * Cambia la contrasena del usuario autenticado. Consume la contrasena temporal
     * si existia y desactiva la obligacion de cambio.
     */
    @Transactional
    public UserResponse changePassword(AppUserPrincipal principal, ChangePasswordRequest request) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("INVALID_CURRENT_PASSWORD", "La contrasena actual no es correcta.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("PASSWORD_UNCHANGED", "La nueva contrasena debe ser diferente a la actual.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setTemporaryPasswordExpiresAt(null);

        AppUserPrincipal refreshed = AppUserPrincipal.from(user);
        UsernamePasswordAuthenticationToken newAuthentication =
                new UsernamePasswordAuthenticationToken(refreshed, refreshed.getPassword(), refreshed.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuthentication);

        return UserResponse.from(user);
    }

    /** Invalida la sesion actual y limpia el contexto de seguridad. */
    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
