package com.smartstock.identity.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = normalizeHeader(request.getHeader(REQUEST_ID_HEADER));
        String correlationId = normalizeHeader(request.getHeader(CORRELATION_ID_HEADER));
        RequestContext requestContext = new RequestContext(
                requestId,
                correlationId,
                request.getRequestURI(),
                resolveIpAddress(request),
                request.getHeader("User-Agent"),
                null,
                null
        );
        RequestContextHolder.set(requestContext);
        MDC.put("requestId", requestId);
        MDC.put("correlationId", correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestContextHolder.clear();
            MDC.remove("requestId");
            MDC.remove("correlationId");
            MDC.remove("userId");
        }
    }

    private String normalizeHeader(String headerValue) {
        return headerValue == null || headerValue.isBlank() ? UUID.randomUUID().toString() : headerValue;
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
