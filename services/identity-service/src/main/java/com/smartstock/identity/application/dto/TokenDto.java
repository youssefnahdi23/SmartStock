package com.smartstock.identity.application.dto;

public record TokenDto(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresInSeconds
) {
}
