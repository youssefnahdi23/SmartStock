package com.smartstock.identity.service;

import com.smartstock.identity.entity.User;
import com.smartstock.identity.repository.PasswordHistoryRepository;
import com.smartstock.identity.repository.UserRepository;
import com.smartstock.identity.service.impl.PasswordServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private PasswordServiceImpl passwordService;
    
    @Test
    void testValidatePasswordSuccess() {
        String validPassword = "ValidPassword123!";
        
        assertDoesNotThrow(() -> {
            passwordService.validatePassword(validPassword);
        });
    }
    
    @Test
    void testValidatePasswordTooShort() {
        String shortPassword = "Short1!";
        
        assertThrows(IllegalArgumentException.class, () -> {
            passwordService.validatePassword(shortPassword);
        });
    }
    
    @Test
    void testValidatePasswordNoUppercase() {
        String noUppercase = "password123!";
        
        assertThrows(IllegalArgumentException.class, () -> {
            passwordService.validatePassword(noUppercase);
        });
    }
    
    @Test
    void testValidatePasswordNoSpecialChar() {
        String noSpecial = "Password1234";
        
        assertThrows(IllegalArgumentException.class, () -> {
            passwordService.validatePassword(noSpecial);
        });
    }
    
    @Test
    void testValidatePasswordEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordService.validatePassword("");
        });
    }
}
