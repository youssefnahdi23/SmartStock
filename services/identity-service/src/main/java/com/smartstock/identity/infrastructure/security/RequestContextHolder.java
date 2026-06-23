package com.smartstock.identity.infrastructure.security;

import java.util.Optional;

public final class RequestContextHolder {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    private RequestContextHolder() {
    }

    public static void set(RequestContext requestContext) {
        CONTEXT.set(requestContext);
    }

    public static Optional<RequestContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
