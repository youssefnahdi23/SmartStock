package com.smartstock.identity.security;

import com.smartstock.identity.entity.User;
import com.smartstock.identity.exception.InvalidTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    
    @Value("${jwt.secret-key}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiration:3600}")
    private long accessTokenExpiration;
    
    @Value("${jwt.refresh-token-expiration:2592000}")
    private long refreshTokenExpiration;
    
    @Value("${jwt.issuer:smartstock-auth}")
    private String issuer;
    
    @Value("${jwt.audience:smartstock-api}")
    private String audience;
    
    private KeyPair keyPair;
    
    public JwtTokenProvider() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(4096);
            this.keyPair = keyGen.generateKeyPair();
        } catch (Exception e) {
            log.error("Failed to generate RSA key pair", e);
            throw new RuntimeException("Failed to initialize JWT provider", e);
        }
    }
    
    public String generateAccessToken(User user, List<String> roles, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        
        return buildToken(claims, user.getId().toString(), accessTokenExpiration, "access");
    }
    
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        
        return buildToken(claims, user.getId().toString(), refreshTokenExpiration, "refresh");
    }
    
    private String buildToken(Map<String, Object> claims, String subject, long expirationTime, String tokenType) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationTime);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiration))
            .setIssuer(issuer)
            .setAudience(audience)
            .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(keyPair.getPublic())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw new InvalidTokenException("Token has expired", e);
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            throw new InvalidTokenException("Unsupported JWT token", e);
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid JWT token", e);
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            throw new InvalidTokenException("JWT signature validation failed", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            throw new InvalidTokenException("JWT claims string is empty", e);
        }
    }
    
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(keyPair.getPublic())
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (JwtException e) {
            log.error("Failed to extract claims from JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Failed to extract claims from token", e);
        }
    }
    
    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }
    
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (List<String>) claims.get("roles");
    }
    
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (List<String>) claims.get("permissions");
    }
    
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (InvalidTokenException e) {
            return true;
        }
    }
    
    public long getExpirationTimeInSeconds() {
        return accessTokenExpiration;
    }
    
    public Key getSigningKey() {
        return keyPair.getPrivate();
    }
    
    public Key getVerificationKey() {
        return keyPair.getPublic();
    }
}
