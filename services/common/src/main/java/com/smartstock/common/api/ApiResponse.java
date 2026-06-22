package com.smartstock.common.api;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Standard API response wrapper for all endpoints.
 * Ensures consistency across all microservices.
 * Follows REST API guidelines (ADR-0016).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int status;
    private String error;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String path;
    private String requestId;
    private String correlationId;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .data(data)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Success");
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .status(201)
                .data(data)
                .message("Resource created successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiResponse<?> error(int status, String error, String message) {
        return ApiResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiResponse<?> badRequest(String message) {
        return error(400, "Bad Request", message);
    }

    public static ApiResponse<?> unauthorized(String message) {
        return error(401, "Unauthorized", message);
    }

    public static ApiResponse<?> forbidden(String message) {
        return error(403, "Forbidden", message);
    }

    public static ApiResponse<?> notFound(String message) {
        return error(404, "Not Found", message);
    }

    public static ApiResponse<?> internalServerError(String message) {
        return error(500, "Internal Server Error", message);
    }
}
