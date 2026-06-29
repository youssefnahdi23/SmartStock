package com.smartstock.common.test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Generates real, signed JWT tokens for integration and controller tests.
 * The secret must match the value in {@code application-test.yml}.
 */
public final class TestJwtTokenFactory {

    public static final String TEST_SECRET =
            "TestSecretKeyForSmartStockIdentityServiceUnitTestingOnlyDoNotUseInProduction2026Long";

    public static final String TEST_USER_ID    = "test-user-001";
    public static final String TEST_ADMIN_ID   = "test-admin-001";
    public static final String TEST_USERNAME   = "test.user";
    public static final String TEST_ADMIN_NAME = "system.admin";

    private static final long EXPIRY_MS = 3_600_000L;

    private TestJwtTokenFactory() {}

    public static String adminToken() {
        return build(TEST_ADMIN_ID, TEST_ADMIN_NAME, List.of("ROLE_ADMIN", "ROLE_MANAGER"));
    }

    public static String managerToken() {
        return build("test-manager-001", "test.manager", List.of("ROLE_MANAGER"));
    }

    public static String userToken() {
        return build(TEST_USER_ID, TEST_USERNAME, List.of("ROLE_USER"));
    }

    public static String expiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(TEST_USER_ID)
                .claim("username", TEST_USERNAME)
                .claim("roles", List.of("ROLE_USER"))
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(key)
                .compact();
    }

    public static String tokenForUser(String userId, String username, List<String> roles) {
        return build(userId, username, roles);
    }

    private static String build(String subject, String username, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claims(Map.of("username", username, "roles", roles))
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRY_MS))
                .issuer("smartstock-auth-test")
                .audience().add("smartstock-api-test").and()
                .signWith(key)
                .compact();
    }
}
