package com.smartstock.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartstock.gateway.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationGlobalFilterTest {

    private static final String SECRET =
            "TestSecretKeyForUnitTestingMustBeAtLeast64BytesLongForHS512AlgorithmPaddingPaddingPadding";

    private JwtAuthenticationGlobalFilter filter;
    private GatewayFilterChain chain;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setIssuer("smartstock-auth");
        props.setAudience("smartstock-api");

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        filter = new JwtAuthenticationGlobalFilter(props, mapper);
        chain = mock(GatewayFilterChain.class);
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());

        byte[] keyBytes = SECRET.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) keyBytes = Arrays.copyOf(keyBytes, 64);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    void allowsPublicAuthPaths() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void allowsActuatorPaths() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void rejects401WhenNoAuthorizationHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void rejects401WhenMalformedBearerHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void rejects401WhenTokenIsExpired() {
        String expired = Jwts.builder()
                .subject("user-123")
                .claim("username", "john")
                .claim("email", "john@example.com")
                .claim("roles", List.of("ROLE_USER"))
                .claim("permissions", List.of("READ"))
                .issuer("smartstock-auth")
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void rejects401WhenSignatureInvalid() {
        byte[] wrongKey = Arrays.copyOf("WrongKeyWrongKeyWrongKeyWrongKeyWrongKeyWrongKeyWrongKeyWrongKeyWWWWWW"
                .getBytes(StandardCharsets.UTF_8), 64);
        SecretKey bad = Keys.hmacShaKeyFor(wrongKey);

        String token = Jwts.builder()
                .subject("user-123")
                .claim("roles", List.of("ROLE_USER"))
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(bad, Jwts.SIG.HS512)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void propagatesUserContextHeadersOnValidToken() {
        String token = Jwts.builder()
                .subject("user-abc-123")
                .claim("username", "alice")
                .claim("email", "alice@example.com")
                .claim("roles", List.of("ROLE_ADMIN", "ROLE_USER"))
                .claim("permissions", List.of("READ", "WRITE"))
                .issuer("smartstock-auth")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        var captor = forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);

        ServerWebExchange captured = captor.getValue();
        HttpHeaders headers = captured.getRequest().getHeaders();

        assertThat(headers.getFirst("X-User-Id")).isEqualTo("user-abc-123");
        assertThat(headers.getFirst("X-User-Name")).isEqualTo("alice");
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("alice@example.com");
        assertThat(headers.getFirst("X-User-Roles")).contains("ROLE_ADMIN");
        assertThat(headers.getFirst("X-User-Permissions")).contains("READ");
    }

    @Test
    void hasCorrectFilterOrder() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
    }
}
