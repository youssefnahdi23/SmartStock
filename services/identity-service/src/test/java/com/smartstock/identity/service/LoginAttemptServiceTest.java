package com.smartstock.identity.service;

import com.smartstock.identity.entity.FailedLoginAttempt;
import com.smartstock.identity.entity.User;
import com.smartstock.identity.exception.UserLockedException;
import com.smartstock.identity.repository.FailedLoginAttemptRepository;
import com.smartstock.identity.repository.UserRepository;
import com.smartstock.identity.service.impl.LoginAttemptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {
    
    @Mock
    private FailedLoginAttemptRepository failedLoginAttemptRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private LoginAttemptServiceImpl loginAttemptService;
    
    @Test
    void testCheckLoginAttemptsWithNoFailures() {
        when(failedLoginAttemptRepository.findByUsername("testuser"))
            .thenReturn(Optional.empty());
        
        assertDoesNotThrow(() -> {
            loginAttemptService.checkLoginAttempts("testuser");
        });
    }
    
    @Test
    void testCheckLoginAttemptsWithExpiredLockout() {
        FailedLoginAttempt attempt = FailedLoginAttempt.builder()
            .username("testuser")
            .attemptCount(5)
            .lockedUntil(LocalDateTime.now().minusMinutes(1))
            .build();
        
        when(failedLoginAttemptRepository.findByUsername("testuser"))
            .thenReturn(Optional.of(attempt));
        
        assertDoesNotThrow(() -> {
            loginAttemptService.checkLoginAttempts("testuser");
        });
    }
    
    @Test
    void testCheckLoginAttemptsWithActiveLockout() {
        FailedLoginAttempt attempt = FailedLoginAttempt.builder()
            .username("testuser")
            .attemptCount(5)
            .lockedUntil(LocalDateTime.now().plusMinutes(30))
            .build();
        
        when(failedLoginAttemptRepository.findByUsername("testuser"))
            .thenReturn(Optional.of(attempt));
        
        assertThrows(UserLockedException.class, () -> {
            loginAttemptService.checkLoginAttempts("testuser");
        });
    }
    
    @Test
    void testRecordFailedAttempt() {
        when(failedLoginAttemptRepository.findByUsername("testuser"))
            .thenReturn(Optional.empty());
        
        loginAttemptService.recordFailedAttempt("testuser");
        
        verify(failedLoginAttemptRepository).save(any(FailedLoginAttempt.class));
    }
    
    @Test
    void testClearFailedAttempts() {
        loginAttemptService.clearFailedAttempts("testuser");
        
        verify(failedLoginAttemptRepository).deleteByUsername("testuser");
    }
}
