package com.smartstock.identity.infrastructure.security;

import com.smartstock.identity.domain.model.Permission;
import com.smartstock.identity.domain.model.Role;
import com.smartstock.identity.domain.model.User;
import com.smartstock.identity.infrastructure.config.SecurityProperties;
import com.smartstock.identity.presentation.exception.InvalidTokenException;
import com.smartstock.identity.presentation.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    public static final String TOKEN_TYPE_BEARER = "Bearer";
    public static final String CLAIM_TYPE = "type";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_WAREHOUSE_IDS = "warehouseIds";

    private final SecurityProperties securityProperties;
    private final JwtKeyPairGenerator jwtKeyPairGenerator;
    private final Clock clock;

    public JwtTokenProvider(SecurityProperties securityProperties,
                            JwtKeyPairGenerator jwtKeyPairGenerator,
                            Clock clock) {
        this.securityProperties = securityProperties;
        this.jwtKeyPairGenerator = jwtKeyPairGenerator;
        this.clock = clock;
    }

    public SignedToken generateAccessToken(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(securityProperties.getJwt().getAccessTokenValidity());
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuer(securityProperties.getJwt().getIssuer())
                .audience().add(securityProperties.getJwt().getAudience()).and()
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(tokenId)
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLES, extractRoles(user))
                .claim(CLAIM_PERMISSIONS, extractPermissions(user))
                .claim(CLAIM_WAREHOUSE_IDS, user.getWarehouseIds().stream().sorted().toList())
                .signWith(jwtKeyPairGenerator.privateKey(), SignatureAlgorithm.RS256)
                .compact();
        return new SignedToken(token, tokenId, expiresAt, TOKEN_TYPE_BEARER);
    }

    public SignedToken generateRefreshToken(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(securityProperties.getJwt().getRefreshTokenValidity());
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuer(securityProperties.getJwt().getIssuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(tokenId)
                .claim(CLAIM_TYPE, "refresh")
                .signWith(jwtKeyPairGenerator.privateKey(), SignatureAlgorithm.RS256)
                .compact();
        return new SignedToken(token, tokenId, expiresAt, TOKEN_TYPE_BEARER);
    }

    public AuthenticatedUser toAuthenticatedUser(String token) {
        Claims claims = parseAccessToken(token).getPayload();
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                claims.get(CLAIM_USERNAME, String.class),
                claims.get(CLAIM_EMAIL, String.class),
                claims.get(CLAIM_ROLES, List.class),
                claims.get(CLAIM_PERMISSIONS, List.class),
                claims.get(CLAIM_WAREHOUSE_IDS, List.class)
        );
    }

    public Jws<Claims> parseAccessToken(String token) {
        Jws<Claims> claimsJws = parse(token);
        if ("refresh".equals(claimsJws.getPayload().get(CLAIM_TYPE, String.class))) {
            throw new InvalidTokenException("Refresh tokens cannot be used as access tokens.");
        }
        validateAudience(claimsJws.getPayload());
        return claimsJws;
    }

    public Jws<Claims> parseRefreshToken(String token) {
        Jws<Claims> claimsJws = parse(token);
        if (!"refresh".equals(claimsJws.getPayload().get(CLAIM_TYPE, String.class))) {
            throw new InvalidTokenException("Token is not a refresh token.");
        }
        return claimsJws;
    }

    public long accessTokenTtlSeconds() {
        return securityProperties.getJwt().getAccessTokenValidity().toSeconds();
    }

    private Jws<Claims> parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(jwtKeyPairGenerator.publicKey())
                    .requireIssuer(securityProperties.getJwt().getIssuer())
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException exception) {
            throw new TokenExpiredException("Token has expired.");
        } catch (MalformedJwtException | UnsupportedJwtException | SignatureException | IllegalArgumentException exception) {
            throw new InvalidTokenException("Token is invalid.");
        }
    }

    private void validateAudience(Claims claims) {
        if (claims.getAudience() == null || !claims.getAudience().contains(securityProperties.getJwt().getAudience())) {
            throw new InvalidTokenException("Token audience is invalid.");
        }
    }

    private List<String> extractRoles(User user) {
        return user.getRoles().stream()
                .filter(Role::isActive)
                .map(Role::getName)
                .sorted()
                .toList();
    }

    private List<String> extractPermissions(User user) {
        return user.getRoles().stream()
                .filter(Role::isActive)
                .map(Role::getPermissions)
                .flatMap(Set::stream)
                .filter(Permission::isActive)
                .map(Permission::getPermissionKey)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    public record SignedToken(
            String token,
            String tokenId,
            Instant expiresAt,
            String tokenType
    ) {
    }
}
