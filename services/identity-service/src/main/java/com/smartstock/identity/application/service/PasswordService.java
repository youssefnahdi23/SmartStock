package com.smartstock.identity.application.service;

import com.smartstock.identity.domain.model.PasswordHistory;
import com.smartstock.identity.domain.model.User;
import com.smartstock.identity.domain.repository.PasswordHistoryRepository;
import com.smartstock.identity.infrastructure.config.SecurityProperties;
import com.smartstock.identity.presentation.exception.PasswordPolicyViolationException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordService {

    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    public PasswordService(PasswordEncoder passwordEncoder,
                           PasswordHistoryRepository passwordHistoryRepository,
                           SecurityProperties securityProperties,
                           Clock clock) {
        this.passwordEncoder = passwordEncoder;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.securityProperties = securityProperties;
        this.clock = clock;
    }

    public EncodedPassword preparePassword(User user, String rawPassword) {
        validatePasswordPolicy(rawPassword);
        if (user != null) {
            ensurePasswordNotReused(user, rawPassword);
        }
        Instant changedAt = clock.instant();
        return new EncodedPassword(
                passwordEncoder.encode(rawPassword),
                changedAt,
                changedAt.plus(securityProperties.getPassword().getExpiration())
        );
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public boolean isPasswordExpired(User user) {
        return user.isPasswordExpired(clock);
    }

    @Transactional
    public void recordPasswordHistory(User user, String encodedPassword) {
        passwordHistoryRepository.save(new PasswordHistory(user, encodedPassword));
    }

    public void validatePasswordPolicy(String rawPassword) {
        SecurityProperties.Password passwordProperties = securityProperties.getPassword();
        if (rawPassword == null || rawPassword.length() < passwordProperties.getMinimumLength()) {
            throw new PasswordPolicyViolationException("Password must be at least " + passwordProperties.getMinimumLength() + " characters long.");
        }
        if (passwordProperties.isRequireUppercase() && rawPassword.chars().noneMatch(Character::isUpperCase)) {
            throw new PasswordPolicyViolationException("Password must contain at least one uppercase character.");
        }
        if (passwordProperties.isRequireLowercase() && rawPassword.chars().noneMatch(Character::isLowerCase)) {
            throw new PasswordPolicyViolationException("Password must contain at least one lowercase character.");
        }
        if (passwordProperties.isRequireDigit() && rawPassword.chars().noneMatch(Character::isDigit)) {
            throw new PasswordPolicyViolationException("Password must contain at least one numeric character.");
        }
        if (passwordProperties.isRequireSpecialCharacter() && rawPassword.chars().allMatch(Character::isLetterOrDigit)) {
            throw new PasswordPolicyViolationException("Password must contain at least one special character.");
        }
    }

    private void ensurePasswordNotReused(User user, String rawPassword) {
        if (matches(rawPassword, user.getPasswordHash())) {
            throw new PasswordPolicyViolationException("Password cannot reuse the current password.");
        }
        List<PasswordHistory> history = passwordHistoryRepository.findTop5ByUser_IdOrderByCreatedAtDesc(user.getId());
        int historyLimit = securityProperties.getPassword().getHistorySize();
        for (int index = 0; index < Math.min(historyLimit, history.size()); index++) {
            if (matches(rawPassword, history.get(index).getPasswordHash())) {
                throw new PasswordPolicyViolationException("Password cannot reuse the last " + historyLimit + " passwords.");
            }
        }
    }

    public record EncodedPassword(String hash, Instant changedAt, Instant expiresAt) {
        public EncodedPassword {
            Objects.requireNonNull(hash, "hash");
            Objects.requireNonNull(changedAt, "changedAt");
            Objects.requireNonNull(expiresAt, "expiresAt");
        }
    }
}
