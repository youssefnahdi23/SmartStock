package com.smartstock.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Structured request/response logging with timing, user context, and correlation ID.
 * Sensitive paths (e.g. /auth/login) have their bodies redacted — only metadata is logged.
 */
@Slf4j
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startMs = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        String correlationId = exchange.getRequest().getHeaders()
                .getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);
        String userId = request.getHeaders().getFirst("X-User-Id");
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        log.info("REQ  method={} path={} correlationId={} userId={}",
                method, path, correlationId, nullSafe(userId));

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long durationMs = System.currentTimeMillis() - startMs;
            int status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : 0;

            if (status >= 500) {
                log.error("RESP method={} path={} status={} durationMs={} correlationId={} userId={}",
                        method, path, status, durationMs, correlationId, nullSafe(userId));
            } else if (status >= 400) {
                log.warn("RESP method={} path={} status={} durationMs={} correlationId={} userId={}",
                        method, path, status, durationMs, correlationId, nullSafe(userId));
            } else {
                log.info("RESP method={} path={} status={} durationMs={} correlationId={} userId={}",
                        method, path, status, durationMs, correlationId, nullSafe(userId));
            }
        }));
    }

    private static String nullSafe(String value) {
        return value != null ? value : "anonymous";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
