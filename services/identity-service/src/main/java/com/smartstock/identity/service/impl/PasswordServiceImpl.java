package com.smartstock.identity.service.impl;

import com.smartstock.identity.entity.PasswordHistory;
import com.smartstock.identity.entity.User;
import com.smartstock.identity.repository.PasswordHistoryRepository;
import com.smartstock.identity.repository.UserRepository;
import com.smartstock.identity.service.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {
    
    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final UserRepository userRepository;
    
    private static final String PASSWORD_REGEX = 
        "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$";
    
    private static final int PASSWORD_HISTORY_COUNT = 5;
    
    @Override
    public void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        
        if (password.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters");
        }
        
        if (!Pattern.matches(PASSWORD_REGEX, password)) {
            throw new IllegalArgumentException(
                "Password must contain uppercase letter, lowercase letter, digit, and special character"
            );
        }
    }
    
    @Override
    public void validatePasswordNotReused(UUID userId, String password) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<PasswordHistory> history = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        for (PasswordHistory ph : history.stream().limit(PASSWORD_HISTORY_COUNT).toList()) {
            if (passwordEncoder.matches(password, ph.getPasswordHash())) {
                throw new IllegalArgumentException(
                    "Password cannot be the same as one of your last " + PASSWORD_HISTORY_COUNT + " passwords"
                );
            }
        }
    }
    
    @Override
    public void addPasswordToHistory(UUID userId, String rawPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        PasswordHistory history = PasswordHistory.builder()
            .user(user)
            .passwordHash(passwordEncoder.encode(rawPassword))
            .build();
        
        passwordHistoryRepository.save(history);
        
        // Keep only last 5 passwords
        List<PasswordHistory> allHistory = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (allHistory.size() > PASSWORD_HISTORY_COUNT) {
            passwordHistoryRepository.deleteAll(allHistory.subList(PASSWORD_HISTORY_COUNT, allHistory.size()));
        }
    }
}
