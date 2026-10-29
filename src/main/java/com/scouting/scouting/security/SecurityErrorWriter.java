package com.scouting.scouting.security;

import com.scouting.scouting.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Escribe respuestas JSON de error desde la cadena de filtros de seguridad,
 * sin pasar por los controladores.
 */
@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, int status, String code, String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String path = request.getRequestURI();
        if (path != null && (path.equals("/api/v1/prospects") || path.startsWith("/api/v1/prospects/")
                || path.equals("/api/v1/recommendations") || path.startsWith("/api/v1/recommendations/")
                || path.equals("/api/v1/clubs/me/recommendations")
                || path.startsWith("/api/v1/clubs/me/recommendations/")
                || path.equals("/api/v1/admin/recommendations")
                || path.startsWith("/api/v1/admin/recommendations/"))) {
            response.setHeader("Cache-Control", "no-store");
        }
        ApiError error = ApiError.of(status, code, message, request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), error);
    }
}
