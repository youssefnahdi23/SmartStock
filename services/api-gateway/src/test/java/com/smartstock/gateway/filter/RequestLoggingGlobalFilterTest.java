package com.smartstock.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLoggingGlobalFilterTest {

    private RequestLoggingGlobalFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingGlobalFilter();
        chain = mock(GatewayFilterChain.class);
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void delegatesToChain() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER, "trace-001")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void doesNotAlterRequestOrResponse() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users")
                .header("X-User-Id", "user-42")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Response status should remain unchanged (no modifications by logging filter)
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void logsWithoutErrorOnMissingHeaders() {
        // No correlation ID or user-id headers — should not throw
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void hasCorrectFilterOrder() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 2);
    }
}
