package com.smartstock.identity.service;

import com.smartstock.identity.api.dto.request.ForgotPasswordRequest;
import com.smartstock.identity.api.dto.request.LoginRequest;
import com.smartstock.identity.api.dto.request.RefreshTokenRequest;
import com.smartstock.identity.api.dto.request.ResetPasswordRequest;
import com.smartstock.identity.api.dto.response.AuthResponse;
import com.smartstock.identity.api.dto.response.UserResponse;
import com.smartstock.identity.config.JwtProperties;
import com.smartstock.identity.domain.model.PasswordResetToken;
import com.smartstock.identity.domain.model.RefreshToken;
import com.smartstock.identity.domain.model.User;
import com.smartstock.identity.domain.repository.PasswordResetTokenRepository;
import com.smartstock.identity.domain.repository.RefreshTokenRepository;
import com.smartstock.identity.domain.repository.UserRepository;
import com.smartstock.identity.exception.*;
import com.smartstock.identity.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final AuditLogService auditLogService;

    @Value("${app.security.max-failed-login-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.lockout-duration-minutes:30}")
    private int lockoutMinutes;

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByUsernameAndNotDeleted(request.getUsername())
                .orElseThrow(() -> {
                    auditLogService.log("LOGIN_FAILED", "User", null, null,
                            "LOGIN_FAILED", "FAILED", ipAddress, userAgent,
                            "User not found: " + request.getUsername());
                    return new InvalidCredentialsException();
                });

        if (user.isAccountLocked()) {
            auditLogService.log("ACCOUNT_LOCKED", "User", user.getId(), user.getId(),
                    "LOGIN_FAILED", "FAILED", ipAddress, userAgent, "Account locked");
            throw new AccountLockedException();
        }

        if (!user.isActive()) {
            throw new BusinessException("ACCOUNT_INACTIVE",
                    "Account is deactivated", HttpStatus.UNAUTHORIZED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, ipAddress, userAgent);
            throw new InvalidCredentialsException();
        }

        user.recordLogin();
        userRepository.save(user);

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Revoke any existing refresh token, issue new one
        refreshTokenRepository.revokeAllByUserId(user.getId());
        RefreshToken storedToken = RefreshToken.builder()
                .token(refreshToken)
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusNanos(
                        jwtProperties.getRefreshTokenExpirationMs() * 1_000_000L))
                .build();
        refreshTokenRepository.save(storedToken);

        auditLogService.log("LOGIN", "User", user.getId(), user.getId(),
                "LOGIN", "SUCCESS", ipAddress, userAgent, null);

        UserResponse userResponse = userService.toResponse(user);
        long expiresInSeconds = jwtProperties.getAccessTokenExpirationMs() / 1000;
        return AuthResponse.of(accessToken, refreshToken, expiresInSeconds, userResponse);
    }

    @Transactional
    public void logout(String userId, String ipAddress, String userAgent) {
        refreshTokenRepository.revokeAllByUserId(userId);
        auditLogService.log("LOGOUT", "User", userId, userId,
                "LOGOUT", "SUCCESS", ipAddress, userAgent, null);
        log.info("User {} logged out", userId);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenAndNotRevoked(request.getRefreshToken())
                .orElseThrow(TokenRevokedException::new);

        if (!stored.isValid()) {
            stored.revoke();
            refreshTokenRepository.save(stored);
            throw new InvalidTokenException("refresh");
        }

        User user = userRepository.findByIdAndNotDeleted(stored.getUserId())
                .orElseThrow(() -> new UserNotFoundException(stored.getUserId()));

        String newAccessToken = jwtService.generateAccessToken(user);
        long expiresInSeconds = jwtProperties.getAccessTokenExpirationMs() / 1000;
        return AuthResponse.refreshed(newAccessToken, expiresInSeconds);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailAndNotDeleted(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        // Invalidate existing tokens
        passwordResetTokenRepository.invalidateAllForUser(user.getId());

        String rawToken = UUID.randomUUID().toString().replace("-", "") +
                          UUID.randomUUID().toString().replace("-", "");

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(rawToken)
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // In production, emit event so Notification Service sends the email.
        // The token is logged at DEBUG only and must never appear in production logs.
        log.debug("Password reset token generated for userId={}", user.getId());
        log.info("Password reset requested for email={}", request.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findValidToken(request.getToken())
                .orElseThrow(() -> new BusinessException(
                        "INVALID_RESET_TOKEN",
                        "Password reset token is invalid or has expired",
                        HttpStatus.BAD_REQUEST));

        if (!resetToken.isValid()) {
            throw new BusinessException("INVALID_RESET_TOKEN",
                    "Password reset token has expired", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByIdAndNotDeleted(resetToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(resetToken.getUserId()));

        userService.resetPasswordWithToken(user, request.getNewPassword());
        resetToken.markUsed();
        passwordResetTokenRepository.save(resetToken);

        // Revoke all refresh tokens after password reset
        refreshTokenRepository.revokeAllByUserId(user.getId());

        auditLogService.log("PASSWORD_RESET", "User", user.getId(), user.getId(),
                "PASSWORD_RESET", "SUCCESS", null, null, null);

        log.info("Password reset completed for userId={}", user.getId());
    }

    // --------------------------------------------------------
    // Private helpers
    // --------------------------------------------------------

    private void handleFailedLogin(User user, String ipAddress, String userAgent) {
        user.recordFailedLogin();

        if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
            user.lock(LocalDateTime.now().plusMinutes(lockoutMinutes));
            userRepository.save(user);
            auditLogService.log("ACCOUNT_LOCKED", "User", user.getId(), user.getId(),
                    "ACCOUNT_LOCKED", "SUCCESS", ipAddress, userAgent,
                    "Locked after " + maxFailedAttempts + " failed attempts");
        } else {
            userRepository.save(user);
            auditLogService.log("LOGIN_FAILED", "User", user.getId(), user.getId(),
                    "LOGIN_FAILED", "FAILED", ipAddress, userAgent,
                    "Failed attempt " + user.getFailedLoginAttempts());
        }
    }
}
