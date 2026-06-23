package com.smartstock.identity.service;

import com.smartstock.identity.dto.LoginRequest;
import com.smartstock.identity.dto.TokenResponse;
import com.smartstock.identity.entity.Role;
import com.smartstock.identity.entity.User;
import com.smartstock.identity.exception.UnauthorizedException;
import com.smartstock.identity.exception.UserLockedException;
import com.smartstock.identity.repository.RefreshTokenRepository;
import com.smartstock.identity.repository.RoleRepository;
import com.smartstock.identity.repository.UserRepository;
import com.smartstock.identity.security.JwtTokenProvider;
import com.smartstock.identity.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private LoginAttemptService loginAttemptService;
    
    @Mock
    private PasswordService passwordService;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private AuthenticationServiceImpl authenticationService;
    
    private User testUser;
    private LoginRequest loginRequest;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("test@example.com")
            .passwordHash("hashedPassword")
            .firstName("Test")
            .lastName("User")
            .isActive(true)
            .isLocked(false)
            .passwordExpiresAt(LocalDateTime.now().plusDays(90))
            .roles(new HashSet<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        loginRequest = LoginRequest.builder()
            .username("testuser")
            .password("password123")
            .build();
    }
    
    @Test
    void testSuccessfulLogin() {
        when(userRepository.findByUsernameAndDeletedAtIsNull("testuser"))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword"))
            .thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), anyList(), anyList()))
            .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any()))
            .thenReturn("refresh-token");
        when(jwtTokenProvider.getExpirationTimeInSeconds())
            .thenReturn(3600L);
        
        TokenResponse response = authenticationService.login(loginRequest);
        
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        
        verify(userRepository).findByUsernameAndDeletedAtIsNull("testuser");
        verify(passwordEncoder).matches("password123", "hashedPassword");
    }
    
    @Test
    void testLoginWithInvalidPassword() {
        when(userRepository.findByUsernameAndDeletedAtIsNull("testuser"))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword"))
            .thenReturn(false);
        
        assertThrows(UnauthorizedException.class, () -> {
            authenticationService.login(loginRequest);
        });
        
        verify(loginAttemptService).recordFailedAttempt("testuser");
    }
    
    @Test
    void testLoginWithInactiveUser() {
        testUser.setIsActive(false);
        
        when(userRepository.findByUsernameAndDeletedAtIsNull("testuser"))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword"))
            .thenReturn(true);
        
        assertThrows(UnauthorizedException.class, () -> {
            authenticationService.login(loginRequest);
        });
        
        verify(loginAttemptService).recordFailedAttempt("testuser");
    }
    
    @Test
    void testLoginWithLockedUser() {
        testUser.setIsLocked(true);
        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        
        when(userRepository.findByUsernameAndDeletedAtIsNull("testuser"))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword"))
            .thenReturn(true);
        
        assertThrows(UserLockedException.class, () -> {
            authenticationService.login(loginRequest);
        });
    }
    
    @Test
    void testValidateToken() {
        when(jwtTokenProvider.validateToken(anyString()))
            .thenReturn(true);
        
        assertDoesNotThrow(() -> {
            authenticationService.validateToken("valid-token");
        });
        
        verify(jwtTokenProvider).validateToken("valid-token");
    }
}
