package com.smartstock.identity.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
