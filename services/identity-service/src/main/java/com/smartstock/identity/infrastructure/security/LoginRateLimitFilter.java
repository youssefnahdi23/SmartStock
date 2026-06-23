package com.smartstock.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.identity.presentation.support.ApiResponseFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginRateLimitService loginRateLimitService;
    private final ObjectMapper objectMapper;
    private final ApiResponseFactory apiResponseFactory;

    public LoginRateLimitFilter(LoginRateLimitService loginRateLimitService,
                                ObjectMapper objectMapper,
                                ApiResponseFactory apiResponseFactory) {
        this.loginRateLimitService = loginRateLimitService;
        this.objectMapper = objectMapper;
        this.apiResponseFactory = apiResponseFactory;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (HttpMethod.POST.matches(request.getMethod()) && "/api/v1/auth/login".equals(request.getRequestURI())) {
            String key = RequestContextHolder.get().map(RequestContext::ipAddress).orElse(request.getRemoteAddr());
            if (!loginRateLimitService.isAllowed(key)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                objectMapper.writeValue(response.getOutputStream(),
                        apiResponseFactory.error(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", "Login rate limit exceeded."));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
