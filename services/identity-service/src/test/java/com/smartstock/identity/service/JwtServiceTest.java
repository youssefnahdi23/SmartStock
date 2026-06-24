package com.smartstock.identity.service;

import com.smartstock.identity.config.JwtProperties;
import com.smartstock.identity.domain.model.Permission;
import com.smartstock.identity.domain.model.Role;
import com.smartstock.identity.domain.model.User;
import com.smartstock.identity.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("TestSecretKeyForSmartStockIdentityServiceUnitTestingOnlyDoNotUseInProduction2026Long");
        properties.setAccessTokenExpirationMs(3600000L);
        properties.setRefreshTokenExpirationMs(2592000000L);
        properties.setIssuer("smartstock-auth-test");
        properties.setAudience("smartstock-api-test");
        jwtService = new JwtService(properties);
    }

    @Test
    void generateAccessToken_shouldReturnValidJwt() {
        User user = buildTestUser();
        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void extractSubject_shouldReturnUserId() {
        User user = buildTestUser();
        String token = jwtService.generateAccessToken(user);

        String subject = jwtService.extractSubject(token);
        assertThat(subject).isEqualTo(user.getId());
    }

    @Test
    void generateRefreshToken_shouldReturnValidJwt() {
        User user = buildTestUser();
        String token = jwtService.generateRefreshToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractSubject(token)).isEqualTo(user.getId());
    }

    @Test
    void isTokenValid_withTamperedToken_shouldReturnFalse() {
        User user = buildTestUser();
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenExpired_withFreshToken_shouldReturnFalse() {
        User user = buildTestUser();
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void isTokenValid_withExpiredToken_shouldReturnFalse() {
        // Use a token with 0ms expiry
        properties.setAccessTokenExpirationMs(0L);
        JwtService expiredService = new JwtService(properties);
        User user = buildTestUser();
        String token = expiredService.generateAccessToken(user);

        assertThat(expiredService.isTokenValid(token)).isFalse();
    }

    @Test
    void extractAllClaims_shouldContainRolesAndPermissions() {
        User user = buildTestUser();
        String token = jwtService.generateAccessToken(user);

        var claims = jwtService.extractAllClaims(token);
        assertThat(claims.get("roles")).isNotNull();
        assertThat(claims.get("permissions")).isNotNull();
        assertThat(claims.get("username")).isEqualTo("testuser");
    }

    // --------------------------------------------------------

    private User buildTestUser() {
        Permission perm = Permission.builder()
                .id("perm-1")
                .name("inventory:read")
                .resource("inventory")
                .action("READ")
                .active(true)
                .build();

        Role role = Role.builder()
                .id("role-1")
                .name("INVENTORY_OPERATOR")
                .active(true)
                .permissions(Set.of(perm))
                .build();

        return User.builder()
                .id("user-test-001")
                .username("testuser")
                .email("testuser@smartstock.local")
                .passwordHash("$2a$12$hashed")
                .firstName("Test")
                .lastName("User")
                .active(true)
                .emailVerified(true)
                .roles(Set.of(role))
                .build();
    }
}
