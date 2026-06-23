package com.smartstock.identity.presentation.advice;

import com.smartstock.common.api.ApiResponse;
import com.smartstock.identity.presentation.exception.AccountLockedException;
import com.smartstock.identity.presentation.exception.ConflictException;
import com.smartstock.identity.presentation.exception.IdentityException;
import com.smartstock.identity.presentation.exception.InvalidTokenException;
import com.smartstock.identity.presentation.exception.PasswordPolicyViolationException;
import com.smartstock.identity.presentation.exception.RateLimitExceededException;
import com.smartstock.identity.presentation.exception.ResourceNotFoundException;
import com.smartstock.identity.presentation.exception.UnauthorizedException;
import com.smartstock.identity.presentation.support.ApiResponseFactory;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ApiResponseFactory apiResponseFactory;

    public GlobalExceptionHandler(ApiResponseFactory apiResponseFactory) {
        this.apiResponseFactory = apiResponseFactory;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return response(HttpStatus.BAD_REQUEST, "Bad Request", message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining("; "));
        return response(HttpStatus.BAD_REQUEST, "Bad Request", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return response(HttpStatus.BAD_REQUEST, "Bad Request", "Request body could not be parsed.");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "Not Found", exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException exception) {
        return response(HttpStatus.CONFLICT, "Conflict", exception.getMessage());
    }

    @ExceptionHandler({UnauthorizedException.class, InvalidTokenException.class, AccountLockedException.class})
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(IdentityException exception) {
        return response(HttpStatus.UNAUTHORIZED, "Unauthorized", exception.getMessage());
    }

    @ExceptionHandler(PasswordPolicyViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handlePasswordPolicy(PasswordPolicyViolationException exception) {
        return response(HttpStatus.BAD_REQUEST, "Bad Request", exception.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(RateLimitExceededException exception) {
        return response(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to access this resource.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred.");
    }

    private ResponseEntity<ApiResponse<Void>> response(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(apiResponseFactory.error(status, error, message));
    }
}
