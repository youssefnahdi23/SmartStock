package com.smartstock.identity.service;

import java.util.UUID;

public interface PasswordService {
    void validatePassword(String password);
    void validatePasswordNotReused(UUID userId, String password);
    void addPasswordToHistory(UUID userId, String rawPassword);
}
