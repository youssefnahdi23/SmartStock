package com.smartstock.identity.infrastructure.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private final Jwt jwt = new Jwt();
    private final Password password = new Password();
    private final LoginAttempts loginAttempts = new LoginAttempts();
    private final RateLimit rateLimit = new RateLimit();
    private final Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {
        private String issuer;
        private String audience;
        private Duration accessTokenValidity = Duration.ofHours(1);
        private Duration refreshTokenValidity = Duration.ofDays(30);
        private boolean generateKeyPair = true;
        private String privateKey;
        private String publicKey;
    }

    @Getter
    @Setter
    public static class Password {
        private int bcryptStrength = 12;
        private int minimumLength = 12;
        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireDigit = true;
        private boolean requireSpecialCharacter = true;
        private int historySize = 5;
        private Duration expiration = Duration.ofDays(90);
    }

    @Getter
    @Setter
    public static class LoginAttempts {
        private int maxAttempts = 5;
        private Duration lockoutDuration = Duration.ofMinutes(30);
        private Duration resetWindow = Duration.ofMinutes(30);
    }

    @Getter
    @Setter
    public static class RateLimit {
        private int loginMaxRequests = 20;
        private Duration loginWindow = Duration.ofMinutes(1);
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedMethods = new ArrayList<>();
        private List<String> allowedHeaders = new ArrayList<>();
        private List<String> exposedHeaders = new ArrayList<>();
        private boolean allowCredentials = true;
    }
}
