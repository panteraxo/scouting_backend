package com.scouting.scouting.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        return error(ex.getStatus(),
                ApiError.of(ex.getStatus().value(), ex.getCode(), ex.getMessage(), request.getRequestURI()),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, ApiError.validation(request.getRequestURI(), fieldErrors), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST,
                ApiError.of(400, "VALIDATION_ERROR", "Datos de entrada invalidos.", request.getRequestURI()), request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleUnreadable(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST,
                ApiError.of(400, "INVALID_REQUEST", "Solicitud invalida.", request.getRequestURI()), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST,
                ApiError.of(400, "INVALID_REQUEST", "Solicitud invalida.", request.getRequestURI()), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN,
                ApiError.of(403, "ACCESS_DENIED", "No tiene permisos para realizar esta accion.",
                        request.getRequestURI()), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED,
                ApiError.of(401, "AUTHENTICATION_REQUIRED", "Credenciales invalidas.",
                        request.getRequestURI()), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Conflicto de integridad de datos en {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());
        return error(HttpStatus.CONFLICT,
                ApiError.of(409, "DATA_CONFLICT", "No se pudo completar la operacion.", request.getRequestURI()),
                request);
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiError> handleLockConflict(Exception ex, HttpServletRequest request) {
        log.warn("Conflicto de bloqueo en {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());
        return error(HttpStatus.CONFLICT,
                ApiError.of(409, "DATA_CONFLICT", "No se pudo completar la operacion por un conflicto concurrente.",
                        request.getRequestURI()), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED,
                ApiError.of(405, "METHOD_NOT_ALLOWED", "Metodo HTTP no permitido.", request.getRequestURI()),
                request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND,
                ApiError.of(404, "NOT_FOUND", "Recurso no encontrado.", request.getRequestURI()), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                ApiError.of(500, "INTERNAL_ERROR", "Ocurrio un error inesperado.", request.getRequestURI()), request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, ApiError body, HttpServletRequest request) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (isNoStoreErrorPath(request)) {
            builder.cacheControl(CacheControl.noStore());
        }
        return builder.body(body);
    }

    private boolean isNoStoreErrorPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        return path.equals("/api/v1/prospects")
                || path.startsWith("/api/v1/prospects/")
                || path.equals("/api/v1/recommendations")
                || path.startsWith("/api/v1/recommendations/")
                || path.equals("/api/v1/clubs/me/recommendations")
                || path.startsWith("/api/v1/clubs/me/recommendations/")
                || path.equals("/api/v1/admin/recommendations")
                || path.startsWith("/api/v1/admin/recommendations/");
    }
}
