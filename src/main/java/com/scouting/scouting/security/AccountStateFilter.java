package com.scouting.scouting.security;

import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/**
 * Filtro de estado de cuenta. En cada request autenticado vuelve a leer la cuenta
 * desde la base de datos para:
 *
 * <ul>
 *   <li>invalidar la sesion cuando el estado deja de ser {@code ACTIVE} o la
 *   contrasena temporal expiro;</li>
 *   <li>limitar una cuenta con cambio de contrasena obligatorio a las operaciones
 *   de autenticacion;</li>
 *   <li>refrescar el principal para que los cambios de estado se apliquen sin
 *   reutilizar datos obsoletos de la sesion.</li>
 * </ul>
 *
 * <p>No se registra como bean de filtro para evitar que Spring Boot lo aplique dos veces;
 * la cadena de seguridad lo instala explicitamente.</p>
 */
public class AccountStateFilter extends OncePerRequestFilter {

    static final String CODE_PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED";

    private static final Set<String> TEMPORARY_PASSWORD_ALLOWED_PATHS = Set.of(
            "/api/v1/auth/csrf",
            "/api/v1/auth/me",
            "/api/v1/auth/logout",
            "/api/v1/auth/change-password"
    );

    private final UserRepository userRepository;
    private final SecurityErrorWriter errorWriter;

    public AccountStateFilter(UserRepository userRepository, SecurityErrorWriter errorWriter) {
        this.userRepository = userRepository;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository.findById(principal.id()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE
                || user.isTemporaryPasswordExpired(Instant.now())) {
            invalidateSession(request);
            errorWriter.write(request, response, 401, "ACCOUNT_NOT_ACTIVE",
                    "La cuenta no esta habilitada.");
            return;
        }

        AppUserPrincipal refreshed = AppUserPrincipal.from(user);
        UsernamePasswordAuthenticationToken refreshedAuthentication =
                new UsernamePasswordAuthenticationToken(refreshed, refreshed.getPassword(), refreshed.getAuthorities());
        refreshedAuthentication.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(refreshedAuthentication);

        if (refreshed.mustChangePassword() && !isAllowedDuringPasswordChange(request)) {
            errorWriter.write(request, response, 403, CODE_PASSWORD_CHANGE_REQUIRED,
                    "Debe cambiar su contrasena antes de continuar.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedDuringPasswordChange(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        return TEMPORARY_PASSWORD_ALLOWED_PATHS.contains(path);
    }

    private void invalidateSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
