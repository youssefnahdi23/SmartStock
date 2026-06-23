package com.smartstock.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.identity.presentation.support.ApiResponseFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final ApiResponseFactory apiResponseFactory;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper, ApiResponseFactory apiResponseFactory) {
        this.objectMapper = objectMapper;
        this.apiResponseFactory = apiResponseFactory;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(),
                apiResponseFactory.error(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication is required to access this resource."));
    }
}
