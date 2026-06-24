package com.smartstock.identity.service;

import com.smartstock.identity.api.dto.request.LoginRequest;
import com.smartstock.identity.api.dto.request.RefreshTokenRequest;
import com.smartstock.identity.api.dto.response.AuthResponse;
import com.smartstock.identity.config.JwtProperties;
import com.smartstock.identity.domain.model.RefreshToken;
import com.smartstock.identity.domain.model.User;
import com.smartstock.identity.domain.repository.PasswordResetTokenRepository;
import com.smartstock.identity.domain.repository.RefreshTokenRepository;
import com.smartstock.identity.domain.repository.UserRepository;
import com.smartstock.identity.exception.*;
import com.smartstock.identity.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private JwtProperties jwtProperties;
    @Mock private UserService userService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutMinutes", 30);
        ReflectionTestUtils.setField(authService, "passwordEncoder", encoder);

        testUser = User.builder()
                .id("user-001")
                .username("testuser")
                .email("test@example.com")
                .passwordHash(encoder.encode("Password123!Test"))
                .firstName("Test")
                .lastName("User")
                .active(true)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .roles(new HashSet<>())
                .build();
    }

    @Test
    void login_withValidCredentials_shouldReturnAuthResponse() {
        when(userRepository.findByUsernameAndNotDeleted("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(2592000000L);
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(3600000L);
        when(userRepository.save(any())).thenReturn(testUser);
        when(userService.toResponse(any())).thenReturn(
                com.smartstock.identity.api.dto.response.UserResponse.builder()
                        .id("user-001").username("testuser").build());

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("Password123!Test");

        AuthResponse response = authService.login(request, "127.0.0.1", "test-agent");

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(refreshTokenRepository).revokeAllByUserId("user-001");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_withWrongPassword_shouldThrowInvalidCredentials() {
        when(userRepository.findByUsernameAndNotDeleted("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("WrongPassword!");

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "agent"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void login_withUnknownUser_shouldThrowInvalidCredentials() {
        when(userRepository.findByUsernameAndNotDeleted("unknown")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsername("unknown");
        request.setPassword("anyPassword");

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "agent"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_withLockedAccount_shouldThrowAccountLocked() {
        testUser.lock(LocalDateTime.now().plusMinutes(25));
        when(userRepository.findByUsernameAndNotDeleted("testuser")).thenReturn(Optional.of(testUser));

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("Password123!Test");

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "agent"))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void login_afterMaxFailedAttempts_shouldLockAccount() {
        testUser.setFailedLoginAttempts(4);
        when(userRepository.findByUsernameAndNotDeleted("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("WrongPassword!");

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "agent"))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(testUser.isAccountLocked()).isTrue();
    }

    @Test
    void logout_shouldRevokeAllRefreshTokens() {
        doNothing().when(refreshTokenRepository).revokeAllByUserId("user-001");

        authService.logout("user-001", "127.0.0.1", "agent");

        verify(refreshTokenRepository).revokeAllByUserId("user-001");
    }

    @Test
    void refresh_withValidToken_shouldReturnNewAccessToken() {
        RefreshToken storedToken = RefreshToken.builder()
                .id("rt-001")
                .token("valid-refresh-token")
                .userId("user-001")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenAndNotRevoked("valid-refresh-token"))
                .thenReturn(Optional.of(storedToken));
        when(userRepository.findByIdAndNotDeleted("user-001")).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(3600000L);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        AuthResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isNull();
    }

    @Test
    void refresh_withRevokedToken_shouldThrowTokenRevokedException() {
        when(refreshTokenRepository.findByTokenAndNotRevoked("revoked-token"))
                .thenReturn(Optional.empty());

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("revoked-token");

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(TokenRevokedException.class);
    }
}
