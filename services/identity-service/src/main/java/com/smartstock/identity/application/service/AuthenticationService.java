package com.smartstock.identity.application.service;

import com.smartstock.identity.application.dto.LoginRequest;
import com.smartstock.identity.application.dto.LoginResponse;
import com.smartstock.identity.application.dto.RefreshTokenResponse;
import com.smartstock.identity.application.dto.TokenDto;
import com.smartstock.identity.application.support.IdentityMapper;
import com.smartstock.identity.domain.event.UserAuthenticatedEvent;
import com.smartstock.identity.domain.model.RefreshToken;
import com.smartstock.identity.domain.model.User;
import com.smartstock.identity.domain.repository.RefreshTokenRepository;
import com.smartstock.identity.domain.repository.UserRepository;
import com.smartstock.identity.infrastructure.security.JwtTokenProvider;
import com.smartstock.identity.infrastructure.security.RequestContextHolder;
import com.smartstock.identity.presentation.exception.InvalidCredentialsException;
import com.smartstock.identity.presentation.exception.InvalidTokenException;
import com.smartstock.identity.presentation.exception.RefreshTokenException;
import com.smartstock.identity.presentation.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordService passwordService;
    private final LoginAttemptService loginAttemptService;
    private final JwtTokenProvider jwtTokenProvider;
    private final IdentityMapper identityMapper;
    private final AuditService auditService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    public AuthenticationService(UserRepository userRepository,
                                 RefreshTokenRepository refreshTokenRepository,
                                 PasswordService passwordService,
                                 LoginAttemptService loginAttemptService,
                                 JwtTokenProvider jwtTokenProvider,
                                 IdentityMapper identityMapper,
                                 AuditService auditService,
                                 ApplicationEventPublisher applicationEventPublisher,
                                 Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordService = passwordService;
        this.loginAttemptService = loginAttemptService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.identityMapper = identityMapper;
        this.auditService = auditService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase();
        Optional<User> candidate = userRepository.findByUsernameIgnoreCaseAndDeletedAtIsNull(username);
        loginAttemptService.assertLoginAllowed(username, candidate);
        User user = candidate.filter(User::isActive)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password."));
        if (!passwordService.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailedAttempt(username, user.getId(), currentIp());
            auditService.log("AUTHENTICATION_FAILED", "LOGIN", "user", user.getId().toString(), "FAILED",
                    Map.of("reason", "invalid_credentials", "username", username),
                    user.getId(), username);
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        loginAttemptService.recordSuccessfulLogin(user);
        JwtTokenProvider.SignedToken accessToken = jwtTokenProvider.generateAccessToken(user);
        JwtTokenProvider.SignedToken refreshToken = jwtTokenProvider.generateRefreshToken(user);
        refreshTokenRepository.revokeActiveTokens(user.getId(), clock.instant());
        refreshTokenRepository.save(new RefreshToken(
                user,
                refreshToken.tokenId(),
                hash(refreshToken.token()),
                refreshToken.expiresAt(),
                clock.instant(),
                currentIp(),
                currentUserAgent()
        ));
        auditService.log("AUTHENTICATION_SUCCEEDED", "LOGIN", "user", user.getId().toString(), "SUCCESS",
                Map.of("username", user.getUsername()),
                user.getId(), user.getUsername());
        applicationEventPublisher.publishEvent(new UserAuthenticatedEvent(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                "identity-service"
        ));
        boolean passwordChangeRequired = passwordService.isPasswordExpired(user);
        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                passwordChangeRequired,
                user.getPasswordExpiresAt(),
                identityMapper.toRoleDtos(user),
                identityMapper.toPermissionDtos(user),
                new TokenDto(accessToken.tokenType(), accessToken.token(), refreshToken.token(), jwtTokenProvider.accessTokenTtlSeconds())
        );
    }

    @Transactional
    public RefreshTokenResponse refresh(String rawRefreshToken) {
        var claims = jwtTokenProvider.parseRefreshToken(rawRefreshToken).getPayload();
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new RefreshTokenException("Refresh token is not recognized."));
        if (!refreshToken.isActive(clock) || !refreshToken.getTokenId().equals(claims.getId())) {
            throw new RefreshTokenException("Refresh token is no longer active.");
        }
        User user = userRepository.findByIdAndDeletedAtIsNull(UUID.fromString(claims.getSubject()))
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("User account is no longer available."));
        refreshToken.revoke(clock.instant());
        refreshToken.markUsed(clock.instant());
        JwtTokenProvider.SignedToken accessToken = jwtTokenProvider.generateAccessToken(user);
        JwtTokenProvider.SignedToken nextRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        refreshTokenRepository.save(new RefreshToken(
                user,
                nextRefreshToken.tokenId(),
                hash(nextRefreshToken.token()),
                nextRefreshToken.expiresAt(),
                clock.instant(),
                currentIp(),
                currentUserAgent()
        ));
        auditService.log("TOKEN_REFRESHED", "REFRESH", "refresh_token", refreshToken.getId().toString(), "SUCCESS",
                Map.of("userId", user.getId().toString()),
                user.getId(), user.getUsername());
        return new RefreshTokenResponse(new TokenDto(accessToken.tokenType(), accessToken.token(), nextRefreshToken.token(), jwtTokenProvider.accessTokenTtlSeconds()));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            token.revoke(clock.instant());
            auditService.log("LOGOUT_COMPLETED", "LOGOUT", "refresh_token", token.getId().toString(), "SUCCESS",
                    Map.of("userId", token.getUser().getId().toString()),
                    token.getUser().getId(), token.getUser().getUsername());
        });
    }

    public void validateAccessToken(String token) {
        jwtTokenProvider.parseAccessToken(token);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new InvalidTokenException("Unable to hash refresh token.");
        }
    }

    private String currentIp() {
        return RequestContextHolder.get().map(context -> context.ipAddress()).orElse("unknown");
    }

    private String currentUserAgent() {
        return RequestContextHolder.get().map(context -> context.userAgent()).orElse("unknown");
    }
}
