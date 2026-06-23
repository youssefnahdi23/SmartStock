package com.smartstock.identity.application.service;

import com.smartstock.identity.domain.model.FailedLoginAttempt;
import com.smartstock.identity.domain.model.User;
import com.smartstock.identity.domain.repository.FailedLoginAttemptRepository;
import com.smartstock.identity.domain.repository.UserRepository;
import com.smartstock.identity.infrastructure.config.SecurityProperties;
import com.smartstock.identity.presentation.exception.AccountLockedException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {

    private final FailedLoginAttemptRepository failedLoginAttemptRepository;
    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    public LoginAttemptService(FailedLoginAttemptRepository failedLoginAttemptRepository,
                               UserRepository userRepository,
                               SecurityProperties securityProperties,
                               Clock clock) {
        this.failedLoginAttemptRepository = failedLoginAttemptRepository;
        this.userRepository = userRepository;
        this.securityProperties = securityProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public void assertLoginAllowed(String username, Optional<User> user) {
        user.filter(candidate -> candidate.isLocked(clock))
                .ifPresent(candidate -> {
                    throw new AccountLockedException("Account is locked due to repeated failed login attempts.");
                });
        failedLoginAttemptRepository.findByUsernameIgnoreCase(username)
                .filter(attempt -> attempt.isLocked(clock))
                .ifPresent(attempt -> {
                    throw new AccountLockedException("Account is locked due to repeated failed login attempts.");
                });
    }

    @Transactional
    public void recordFailedAttempt(String username, UUID userId, String ipAddress) {
        Instant now = clock.instant();
        FailedLoginAttempt failedLoginAttempt = failedLoginAttemptRepository.findByUsernameIgnoreCase(username)
                .orElseGet(() -> new FailedLoginAttempt(username, userId));
        failedLoginAttempt.registerFailure(
                now,
                ipAddress,
                securityProperties.getLoginAttempts().getMaxAttempts(),
                securityProperties.getLoginAttempts().getResetWindow(),
                securityProperties.getLoginAttempts().getLockoutDuration()
        );
        failedLoginAttemptRepository.save(failedLoginAttempt);
        if (userId != null && failedLoginAttempt.getLockExpiresAt() != null) {
            userRepository.findByIdAndDeletedAtIsNull(userId).ifPresent(user -> user.lockUntil(failedLoginAttempt.getLockExpiresAt()));
        }
    }

    @Transactional
    public void recordSuccessfulLogin(User user) {
        failedLoginAttemptRepository.findByUsernameIgnoreCase(user.getUsername())
                .ifPresent(FailedLoginAttempt::reset);
        user.unlock();
        user.recordSuccessfulLogin(clock.instant());
    }
}
