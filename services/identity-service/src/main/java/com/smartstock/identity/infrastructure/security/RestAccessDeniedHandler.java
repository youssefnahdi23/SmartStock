package com.smartstock.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.identity.presentation.support.ApiResponseFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ApiResponseFactory apiResponseFactory;

    public RestAccessDeniedHandler(ObjectMapper objectMapper, ApiResponseFactory apiResponseFactory) {
        this.objectMapper = objectMapper;
        this.apiResponseFactory = apiResponseFactory;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(),
                apiResponseFactory.error(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to access this resource."));
    }
}
