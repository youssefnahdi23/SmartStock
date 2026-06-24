package com.smartstock.identity.api.controller;

import com.smartstock.identity.api.dto.request.*;
import com.smartstock.identity.api.dto.response.AuthResponse;
import com.smartstock.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Tag(name = "Authentication", description = "Login, logout, token refresh, and password reset")
@RestController
@RequestMapping("/identity/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login with username and password")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                      HttpServletRequest httpRequest) {
        AuthResponse auth = authService.login(request,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(wrapData(auth));
    }

    @Operation(summary = "Logout — revokes refresh token")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@AuthenticationPrincipal UserDetails principal,
                                                       HttpServletRequest httpRequest) {
        authService.logout(principal.getUsername(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(wrapData(Map.of("message", "Logout successful")));
    }

    @Operation(summary = "Refresh access token using a valid refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse auth = authService.refresh(request);
        return ResponseEntity.ok(wrapData(auth));
    }

    @Operation(summary = "Request a password reset email")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(wrapData(Map.of("message", "Password reset email sent")));
    }

    @Operation(summary = "Reset password using a reset token")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(wrapData(Map.of("message", "Password reset successful")));
    }

    // --------------------------------------------------------

    private Map<String, Object> wrapData(Object data) {
        return Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString()));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
