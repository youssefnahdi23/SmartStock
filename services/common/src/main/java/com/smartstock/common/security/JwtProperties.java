package com.smartstock.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration bound from {@code app.jwt.*}. Shared across services (debt H-1) — previously
 * copy-pasted into every service's own {@code config} package. Registered by
 * {@link SecurityAutoConfiguration} via {@code @EnableConfigurationProperties}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private long expirationMs = 86400000;
}
