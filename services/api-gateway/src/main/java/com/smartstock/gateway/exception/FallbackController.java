package com.smartstock.gateway.exception;

import com.smartstock.gateway.filter.CorrelationIdGlobalFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Target of every route's {@code fallbackUri: forward:/fallback}. The circuit
 * breaker forwards here when a downstream call fails or the circuit is open;
 * without this endpoint the forward falls through to static-resource resolution
 * and surfaces as a misleading 404.
 */
@Slf4j
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public Mono<ResponseEntity<ErrorResponse>> fallback(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders()
                .getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);

        log.warn("Circuit-breaker fallback served correlationId={} originalPath={}",
                correlationId, exchange.getRequest().getURI().getPath());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .message("Downstream service is unavailable. Please try again later.")
                .path(exchange.getRequest().getURI().getPath())
                .correlationId(correlationId)
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
