package com.smartstock.common.test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Shared domain-neutral test data used across services.
 * Returns new instances to avoid shared-state bugs between tests.
 */
public final class SmartStockTestFixtures {

    private SmartStockTestFixtures() {}

    public static String randomId() {
        return UUID.randomUUID().toString();
    }

    public static String randomSku() {
        return "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static BigDecimal price(double value) {
        return BigDecimal.valueOf(value);
    }

    public static LocalDate futureDate(int daysFromNow) {
        return LocalDate.now().plusDays(daysFromNow);
    }

    public static LocalDate pastDate(int daysAgo) {
        return LocalDate.now().minusDays(daysAgo);
    }

    /** Canonical admin credentials seeded by Flyway in every service's test DB. */
    public static final String ADMIN_USERNAME = "system.admin";
    public static final String ADMIN_PASSWORD = "Admin@SmartStock2026!";

    /** Standard API base path used across all services. */
    public static final String API_V1 = "/api/v1";
}
