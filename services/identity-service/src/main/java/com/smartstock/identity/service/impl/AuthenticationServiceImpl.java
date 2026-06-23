package com.smartstock.identity.service.impl;

import com.smartstock.identity.dto.LoginRequest;
import com.smartstock.identity.dto.RefreshTokenRequest;
import com.smartstock.identity.dto.TokenResponse;
import com.smartstock.identity.entity.RefreshToken;
import com.smartstock.identity.entity.User;
import com.smartstock.identity.exception.*;
import com.smartstock.identity.repository.RefreshTokenRepository;
import com.smartstock.identity.repository.UserRepository;
import com.smartstock.identity.security.JwtTokenProvider;
import com.smartstock.identity.service.AuditService;
import com.smartstock.identity.service.AuthenticationService;
import com.smartstock.identity.service.LoginAttemptService;
import com.smartstock.identity.service.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final PasswordService passwordService;
    private final AuditService auditService;
    
    @Override
    public TokenResponse login(LoginRequest request) {
        log.debug("Processing login for user: {}", request.getUsername());
        
        // Check login attempts
        loginAttemptService.checkLoginAttempts(request.getUsername());
        
        // Find user
        User user = userRepository.findByUsernameAndDeletedAtIsNull(request.getUsername())
            .orElseThrow(() -> {
                loginAttemptService.recordFailedAttempt(request.getUsername());
                auditService.logFailedLogin(request.getUsername(), "User not found");
                return new UnauthorizedException("Invalid username or password");
            });
        
        // Check if user is active
        if (!user.getIsActive()) {
            loginAttemptService.recordFailedAttempt(request.getUsername());
            auditService.logFailedLogin(user.getId().toString(), "User account is inactive");
            throw new UnauthorizedException("User account is inactive");
        }
        
        // Check if user is locked
        if (user.getIsLocked() && user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            auditService.logFailedLogin(user.getId().toString(), "User account is locked");
            throw new UserLockedException("User account is locked until " + user.getLockedUntil());
        } else if (user.getIsLocked()) {
            user.setIsLocked(false);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
        
        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptService.recordFailedAttempt(request.getUsername());
            auditService.logFailedLogin(user.getId().toString(), "Invalid password");
            throw new UnauthorizedException("Invalid username or password");
        }
        
        // Check password expiration
        if (user.getPasswordExpiresAt() != null && user.getPasswordExpiresAt().isBefore(LocalDateTime.now())) {
            auditService.logEvent(user.getId().toString(), "LOGIN_PASSWORD_EXPIRED", "User", user.getId().toString(), "Password has expired");
            throw new PasswordExpiredException("Your password has expired. Please reset it.");
        }
        
        // Clear failed login attempts
        loginAttemptService.clearFailedAttempts(request.getUsername());
        
        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        // Generate tokens
        List<String> roles = user.getRoles().stream()
            .map(r -> r.getName())
            .collect(Collectors.toList());
        
        List<String> permissions = user.getRoles().stream()
            .flatMap(r -> r.getPermissions().stream())
            .map(p -> p.getResource() + ":" + p.getAction() + (p.getScope() != null ? ":" + p.getScope() : ""))
            .collect(Collectors.toList());
        
        String accessToken = jwtTokenProvider.generateAccessToken(user, roles, permissions);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        
        // Store refresh token
        storeRefreshToken(user, refreshToken);
        
        // Audit log
        auditService.logSuccessfulLogin(user.getId().toString(), user.getUsername());
        
        return TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getExpirationTimeInSeconds())
            .userId(user.getId().toString())
            .username(user.getUsername())
            .roles(roles)
            .build();
    }
    
    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Processing token refresh");
        
        try {
            jwtTokenProvider.validateToken(request.getRefreshToken());
            String userId = jwtTokenProvider.getUserIdFromToken(request.getRefreshToken());
            
            User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            if (!user.getIsActive()) {
                throw new UnauthorizedException("User account is inactive");
            }
            
            // Verify refresh token exists and not revoked
            refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(request.getRefreshToken()))
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or has been revoked"));
            
            List<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toList());
            
            List<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getResource() + ":" + p.getAction() + (p.getScope() != null ? ":" + p.getScope() : ""))
                .collect(Collectors.toList());
            
            String newAccessToken = jwtTokenProvider.generateAccessToken(user, roles, permissions);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
            
            // Store new refresh token and revoke old one
            storeRefreshToken(user, newRefreshToken);
            
            auditService.logEvent(user.getId().toString(), "TOKEN_REFRESH", "Token", "", "Token refreshed successfully");
            
            return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTimeInSeconds())
                .userId(user.getId().toString())
                .username(user.getUsername())
                .roles(roles)
                .build();
        } catch (InvalidTokenException e) {
            auditService.logEvent("", "TOKEN_REFRESH_FAILED", "Token", "", "Token refresh failed: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public void logout(String userId) {
        log.debug("Logging out user: {}", userId);
        refreshTokenRepository.deleteByUserIdAndRevokedFalse(java.util.UUID.fromString(userId));
        auditService.logEvent(userId, "LOGOUT", "User", userId, "User logged out");
    }
    
    @Override
    public void validateToken(String token) {
        jwtTokenProvider.validateToken(token);
    }
    
    private void storeRefreshToken(User user, String refreshToken) {
        RefreshToken token = RefreshToken.builder()
            .user(user)
            .tokenHash(hashToken(refreshToken))
            .expiresAt(LocalDateTime.now().plusSeconds(2592000))
            .revoked(false)
            .build();
        refreshTokenRepository.save(token);
    }
    
    private String hashToken(String token) {
        return passwordEncoder.encode(token);
    }
}
