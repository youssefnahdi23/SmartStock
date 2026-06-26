package com.smartstock.customer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Reads pre-validated user context headers forwarded by the API Gateway.
 * ADR-0005: JWT validation is performed exclusively at the gateway;
 * downstream services trust X-User-* headers set by the gateway filter.
 */
@Slf4j
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    static final String HEADER_USER_ID = "X-User-Id";
    static final String HEADER_USER_EMAIL = "X-User-Email";
    static final String HEADER_USER_PERMISSIONS = "X-User-Permissions";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        if (userId != null && !userId.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String email = request.getHeader(HEADER_USER_EMAIL);
            String permissionsHeader = request.getHeader(HEADER_USER_PERMISSIONS);
            List<String> permissions = (permissionsHeader != null && !permissionsHeader.isBlank())
                    ? Arrays.asList(permissionsHeader.split(","))
                    : List.of();

            SecurityUserDetails userDetails = SecurityUserDetails.builder()
                    .userId(userId)
                    .email(email != null ? email : "")
                    .permissions(permissions)
                    .build();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated user {} from gateway headers", userId);
        }

        filterChain.doFilter(request, response);
    }
}
