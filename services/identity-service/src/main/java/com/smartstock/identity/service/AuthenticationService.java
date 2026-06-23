package com.smartstock.identity.service;

import com.smartstock.identity.dto.LoginRequest;
import com.smartstock.identity.dto.RefreshTokenRequest;
import com.smartstock.identity.dto.TokenResponse;
import com.smartstock.identity.entity.User;

public interface AuthenticationService {
    TokenResponse login(LoginRequest request);
    TokenResponse refreshToken(RefreshTokenRequest request);
    void logout(String userId);
    void validateToken(String token);
}
