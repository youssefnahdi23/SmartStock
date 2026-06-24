package com.smartstock.identity.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex,
                                                                        WebRequest request) {
        log.warn("Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(
                ex.getHttpStatus(),
                List.of(Map.of("code", ex.getErrorCode(), "message", ex.getMessage())),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                 WebRequest request) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(Map.of(
                    "code", "VALIDATION_FAILED",
                    "field", fieldError.getField(),
                    "message", fieldError.getDefaultMessage() != null
                            ? fieldError.getDefaultMessage() : "Invalid value"));
        }
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, errors, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex,
                                                                    WebRequest request) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                List.of(Map.of("code", "INSUFFICIENT_PERMISSIONS",
                               "message", "You do not have permission to perform this action")),
                request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex,
                                                                      WebRequest request) {
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                List.of(Map.of("code", "INVALID_CREDENTIALS", "message", "Invalid credentials")),
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                List.of(Map.of("code", "INTERNAL_ERROR",
                               "message", "An unexpected error occurred")),
                request);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status,
                                                                     List<?> errors,
                                                                     WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        Map<String, Object> body = Map.of(
                "errors", errors,
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", status.value(),
                        "path", path));
        return ResponseEntity.status(status).body(body);
    }
}
