package com.smartstock.identity.controller;

import com.smartstock.identity.dto.LoginRequest;
import com.smartstock.identity.dto.RefreshTokenRequest;
import com.smartstock.identity.dto.TokenResponse;
import com.smartstock.identity.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationService authenticationService;
    
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        TokenResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");
        TokenResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = extractUserIdFromToken(token);
        authenticationService.logout(userId);
        log.info("Logout successful for user: {}", userId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authenticationService.validateToken(token);
        return ResponseEntity.ok().build();
    }
    
    private String extractUserIdFromToken(String token) {
        // This will be extracted from JWT by Spring Security context
        return token;
    }
}
