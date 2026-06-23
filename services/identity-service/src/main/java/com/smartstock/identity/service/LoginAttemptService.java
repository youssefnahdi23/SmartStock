package com.smartstock.identity.service;

public interface LoginAttemptService {
    void checkLoginAttempts(String username);
    void recordFailedAttempt(String username);
    void clearFailedAttempts(String username);
}
