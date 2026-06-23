package com.smartstock.identity.service.impl;

import com.smartstock.identity.entity.FailedLoginAttempt;
import com.smartstock.identity.entity.User;
import com.smartstock.identity.exception.UserLockedException;
import com.smartstock.identity.repository.FailedLoginAttemptRepository;
import com.smartstock.identity.repository.UserRepository;
import com.smartstock.identity.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {
    
    private final FailedLoginAttemptRepository failedLoginAttemptRepository;
    private final UserRepository userRepository;
    
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    
    @Override
    public void checkLoginAttempts(String username) {
        Optional<FailedLoginAttempt> attempt = failedLoginAttemptRepository.findByUsername(username);
        
        if (attempt.isPresent()) {
            FailedLoginAttempt failedAttempt = attempt.get();
            
            if (failedAttempt.getLockedUntil() != null && 
                failedAttempt.getLockedUntil().isAfter(LocalDateTime.now())) {
                log.warn("Login attempt for locked account: {}", username);
                throw new UserLockedException(
                    "Account is locked. Please try again after " + failedAttempt.getLockedUntil()
                );
            }
        }
    }
    
    @Override
    public void recordFailedAttempt(String username) {
        Optional<FailedLoginAttempt> attempt = failedLoginAttemptRepository.findByUsername(username);
        
        FailedLoginAttempt failedAttempt = attempt.orElse(FailedLoginAttempt.builder()
            .username(username)
            .attemptCount(0)
            .build());
        
        failedAttempt.setAttemptCount(failedAttempt.getAttemptCount() + 1);
        failedAttempt.setLastAttemptAt(LocalDateTime.now());
        
        if (failedAttempt.getAttemptCount() >= MAX_FAILED_ATTEMPTS) {
            failedAttempt.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            
            // Lock the user account as well
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isPresent()) {
                User u = user.get();
                u.setIsLocked(true);
                u.setLockedUntil(failedAttempt.getLockedUntil());
                userRepository.save(u);
            }
            
            log.warn("Account locked due to max failed attempts: {}", username);
        }
        
        failedLoginAttemptRepository.save(failedAttempt);
    }
    
    @Override
    public void clearFailedAttempts(String username) {
        failedLoginAttemptRepository.deleteByUsername(username);
        log.debug("Failed login attempts cleared for user: {}", username);
    }
}
