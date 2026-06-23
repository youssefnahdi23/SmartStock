package com.smartstock.identity.presentation.support;

import com.smartstock.common.api.ApiResponse;
import com.smartstock.identity.infrastructure.security.RequestContext;
import com.smartstock.identity.infrastructure.security.RequestContextHolder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseFactory {

    public <T> ApiResponse<T> success(HttpStatus status, T data, String message) {
        return base(status, message, data, null);
    }

    public ApiResponse<Void> error(HttpStatus status, String error, String message) {
        return base(status, message, null, error);
    }

    private <T> ApiResponse<T> base(HttpStatus status, String message, T data, String error) {
        RequestContext context = RequestContextHolder.get().orElse(null);
        return ApiResponse.<T>builder()
                .status(status.value())
                .error(error)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.ofInstant(java.time.Instant.now(), ZoneOffset.UTC))
                .path(context != null ? context.path() : null)
                .requestId(context != null ? context.requestId() : null)
                .correlationId(context != null ? context.correlationId() : null)
                .build();
    }
}
