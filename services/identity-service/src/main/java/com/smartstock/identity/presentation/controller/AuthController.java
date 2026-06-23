package com.smartstock.identity.presentation.controller;

import com.smartstock.common.api.ApiResponse;
import com.smartstock.identity.application.dto.LoginRequest;
import com.smartstock.identity.application.dto.LoginResponse;
import com.smartstock.identity.application.dto.LogoutRequest;
import com.smartstock.identity.application.dto.RefreshTokenRequest;
import com.smartstock.identity.application.dto.RefreshTokenResponse;
import com.smartstock.identity.application.service.AuthenticationService;
import com.smartstock.identity.presentation.support.ApiResponseFactory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final ApiResponseFactory apiResponseFactory;

    public AuthController(AuthenticationService authenticationService, ApiResponseFactory apiResponseFactory) {
        this.authenticationService = authenticationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, authenticationService.login(request), "Authentication succeeded."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, authenticationService.refresh(request.refreshToken()), "Token refresh succeeded."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(request.refreshToken());
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, null, "Logout succeeded."));
    }
}
