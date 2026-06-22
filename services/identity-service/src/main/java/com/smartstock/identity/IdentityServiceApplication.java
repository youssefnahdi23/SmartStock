package com.smartstock.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Identity Service Bootstrap.
 * Microservice responsible for:
 * - User authentication and registration
 * - JWT token generation and validation
 * - Role-based access control (RBAC)
 * - Token refresh and revocation
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.smartstock")
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
