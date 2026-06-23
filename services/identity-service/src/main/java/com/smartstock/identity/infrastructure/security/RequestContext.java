package com.smartstock.identity.infrastructure.security;

public record RequestContext(
        String requestId,
        String correlationId,
        String path,
        String ipAddress,
        String userAgent,
        String userId,
        String username
) {
    public RequestContext withAuthenticatedUser(String userId, String username) {
        return new RequestContext(requestId, correlationId, path, ipAddress, userAgent, userId, username);
    }
}
