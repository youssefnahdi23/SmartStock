package com.smartstock.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterConfigTest {

    private RateLimiterConfig config;

    @BeforeEach
    void setUp() {
        config = new RateLimiterConfig();
    }

    @Test
    void userKeyResolver_usesUserIdHeaderWhenPresent() {
        KeyResolver resolver = config.userOrIpKeyResolver();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header("X-User-Id", "user-abc-123")
                .remoteAddress(new java.net.InetSocketAddress("10.0.0.1", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .assertNext(key -> assertThat(key).isEqualTo("user:user-abc-123"))
                .verifyComplete();
    }

    @Test
    void userKeyResolver_fallsBackToIpWhenNoUserId() {
        KeyResolver resolver = config.userOrIpKeyResolver();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.42", 54321))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .assertNext(key -> assertThat(key).startsWith("ip:"))
                .verifyComplete();
    }

    @Test
    void userKeyResolver_usesForwardedForHeaderIp() {
        KeyResolver resolver = config.userOrIpKeyResolver();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header("X-Forwarded-For", "203.0.113.5, 10.0.0.1")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .assertNext(key -> assertThat(key).isEqualTo("ip:203.0.113.5"))
                .verifyComplete();
    }

    @Test
    void ipKeyResolver_alwaysUsesIp() {
        KeyResolver resolver = config.ipKeyResolver();

        MockServerHttpRequest request = MockServerHttpRequest.get("/any")
                .header("X-User-Id", "should-be-ignored")
                .header("X-Forwarded-For", "10.1.2.3")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .assertNext(key -> assertThat(key).isEqualTo("ip:10.1.2.3"))
                .verifyComplete();
    }
}
