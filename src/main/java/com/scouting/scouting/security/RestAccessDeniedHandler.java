package com.scouting.scouting.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Devuelve 403 JSON desde la cadena de seguridad. Distingue los errores CSRF
 * (token ausente o invalido) de las denegaciones de autorizacion.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorWriter errorWriter;

    public RestAccessDeniedHandler(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        if (accessDeniedException instanceof CsrfException) {
            errorWriter.write(request, response, 403, "CSRF_INVALID",
                    "Token CSRF invalido o ausente.");
            return;
        }
        errorWriter.write(request, response, 403, "ACCESS_DENIED", "No tiene permisos para realizar esta accion.");
    }
}
