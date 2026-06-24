package com.smartstock.identity.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String tokenType;
    private UserResponse user;

    @Builder.Default
    private String timestamp = Instant.now().toString();

    public static AuthResponse of(String accessToken, String refreshToken,
                                   long expiresInSeconds, UserResponse user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresInSeconds)
                .tokenType("Bearer")
                .user(user)
                .build();
    }

    public static AuthResponse refreshed(String accessToken, long expiresInSeconds) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .expiresIn(expiresInSeconds)
                .tokenType("Bearer")
                .build();
    }
}
