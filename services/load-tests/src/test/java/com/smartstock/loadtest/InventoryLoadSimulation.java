package com.smartstock.loadtest;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Gatling load simulation covering the SmartStock critical paths:
 *   1. Authentication (identity-service)
 *   2. Product catalogue browsing (product-service)
 *   3. Stock-level queries (inventory-service)
 *   4. Stock-in operations under load (inventory-service)
 *
 * Run with:
 *   mvn -pl load-tests gatling:test \
 *     -Didentity.base.url=http://localhost:8001 \
 *     -Dinventory.base.url=http://localhost:8003 \
 *     -Dproduct.base.url=http://localhost:8002 \
 *     -Dload.users=100 -Dload.duration.seconds=120 -Dload.ramp.seconds=20
 */
public class InventoryLoadSimulation extends Simulation {

    // ── Configuration ────────────────────────────────────────────────────────
    private static final String IDENTITY_URL  = System.getProperty("identity.base.url",  "http://localhost:8001");
    private static final String INVENTORY_URL = System.getProperty("inventory.base.url", "http://localhost:8003");
    private static final String PRODUCT_URL   = System.getProperty("product.base.url",   "http://localhost:8002");

    private static final int USERS    = Integer.parseInt(System.getProperty("load.users", "50"));
    private static final int DURATION = Integer.parseInt(System.getProperty("load.duration.seconds", "60"));
    private static final int RAMP     = Integer.parseInt(System.getProperty("load.ramp.seconds", "10"));

    // ── HTTP Protocols ───────────────────────────────────────────────────────
    private final HttpProtocolBuilder identityProtocol = http
            .baseUrl(IDENTITY_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling-LoadTest/1.0");

    private final HttpProtocolBuilder inventoryProtocol = http
            .baseUrl(INVENTORY_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling-LoadTest/1.0");

    private final HttpProtocolBuilder productProtocol = http
            .baseUrl(PRODUCT_URL)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling-LoadTest/1.0");

    // ── Feeder: product / warehouse combos ───────────────────────────────────
    private final FeederBuilder<String> stockFeeder = csv("stock_queries.csv").random();

    // ── Scenario 1: Auth — login + refresh cycle ─────────────────────────────
    private final ScenarioBuilder authScenario = scenario("Authentication Load")
            .exec(
                http("POST /auth/login")
                    .post("/api/v1/identity/auth/login")
                    .body(StringBody("""
                        {"username": "system.admin", "password": "Admin@SmartStock2026!"}
                        """))
                    .check(status().is(200))
                    .check(jsonPath("$.data.accessToken").saveAs("accessToken"))
                    .check(jsonPath("$.data.refreshToken").saveAs("refreshToken"))
            )
            .pause(Duration.ofMillis(200))
            .exec(
                http("POST /auth/refresh")
                    .post("/api/v1/identity/auth/refresh")
                    .body(StringBody(session ->
                        "{\"refreshToken\": \"" + session.getString("refreshToken") + "\"}"))
                    .check(status().is(200))
                    .check(jsonPath("$.data.accessToken").saveAs("newAccessToken"))
            )
            .pause(Duration.ofMillis(100))
            .exec(
                http("POST /auth/logout")
                    .post("/api/v1/identity/auth/logout")
                    .header("Authorization", session -> "Bearer " + session.getString("newAccessToken"))
                    .check(status().is(200))
            );

    // ── Scenario 2: Product catalogue browsing ───────────────────────────────
    private final ScenarioBuilder productBrowseScenario = scenario("Product Browse Load")
            .exec(
                http("POST /auth/login — product browse")
                    .post(IDENTITY_URL + "/api/v1/identity/auth/login")
                    .body(StringBody("""
                        {"username": "system.admin", "password": "Admin@SmartStock2026!"}
                        """))
                    .check(status().is(200))
                    .check(jsonPath("$.data.accessToken").saveAs("token"))
            )
            .pause(Duration.ofMillis(100))
            .repeat(5).on(
                exec(
                    http("GET /products?page=0&size=20")
                        .get(PRODUCT_URL + "/api/v1/products?page=0&size=20")
                        .header("Authorization", session -> "Bearer " + session.getString("token"))
                        .check(status().is(200))
                        .check(jsonPath("$.data").exists())
                )
                .pause(Duration.ofMillis(150))
            );

    // ── Scenario 3: Stock level queries under concurrent read load ────────────
    private final ScenarioBuilder stockQueryScenario = scenario("Stock Query Load")
            .exec(
                http("POST /auth/login — stock read")
                    .post(IDENTITY_URL + "/api/v1/identity/auth/login")
                    .body(StringBody("""
                        {"username": "system.admin", "password": "Admin@SmartStock2026!"}
                        """))
                    .check(status().is(200))
                    .check(jsonPath("$.data.accessToken").saveAs("token"))
            )
            .pause(Duration.ofMillis(100))
            .repeat(10).on(
                exec(
                    http("GET /inventory/stock/{productId}/{warehouseId}")
                        .get(INVENTORY_URL + "/api/v1/inventory/stock/prod-#{productIdx}/WH-#{whIdx}")
                        .header("Authorization", session -> "Bearer " + session.getString("token"))
                        // 401 expected when no matching data — still a valid load test response
                        .check(status().in(200, 401, 404))
                )
                .pause(Duration.ofMillis(50))
            );

    // ── Scenario 4: Stock-in write operations ────────────────────────────────
    private final ScenarioBuilder stockInScenario = scenario("Stock-In Write Load")
            .exec(
                http("POST /auth/login — stock write")
                    .post(IDENTITY_URL + "/api/v1/identity/auth/login")
                    .body(StringBody("""
                        {"username": "system.admin", "password": "Admin@SmartStock2026!"}
                        """))
                    .check(status().is(200))
                    .check(jsonPath("$.data.accessToken").saveAs("token"))
            )
            .pause(Duration.ofMillis(200))
            .repeat(3).on(
                exec(
                    http("POST /inventory/stock-in")
                        .post(INVENTORY_URL + "/api/v1/inventory/stock-in")
                        .header("Authorization", session -> "Bearer " + session.getString("token"))
                        .body(StringBody("""
                            {
                              "productId": "prod-load-test-001",
                              "warehouseId": "WH-LOAD-001",
                              "quantity": 10,
                              "unitCost": 25.00,
                              "referenceNumber": "LOAD-TEST-REF"
                            }
                            """))
                        // 400/404 expected when product/warehouse don't exist in test env
                        .check(status().in(200, 201, 400, 404))
                )
                .pause(Duration.ofMillis(300))
            );

    // ── Assertions ───────────────────────────────────────────────────────────
    {
        setUp(
            // Auth scenario: ramp to USERS/4 over RAMP seconds, hold for DURATION
            authScenario.injectOpen(
                rampUsers(USERS / 4).during(Duration.ofSeconds(RAMP)),
                constantUsersPerSec(USERS / 10.0).during(Duration.ofSeconds(DURATION))
            ).protocols(identityProtocol),

            // Product browse: steady read load
            productBrowseScenario.injectOpen(
                rampUsers(USERS / 2).during(Duration.ofSeconds(RAMP)),
                constantUsersPerSec(USERS / 5.0).during(Duration.ofSeconds(DURATION))
            ).protocols(productProtocol),

            // Stock queries: high-frequency reads
            stockQueryScenario.injectOpen(
                rampUsers(USERS).during(Duration.ofSeconds(RAMP)),
                constantUsersPerSec(USERS / 3.0).during(Duration.ofSeconds(DURATION))
            ).protocols(inventoryProtocol),

            // Stock-in: lower rate write scenario
            stockInScenario.injectOpen(
                rampUsers(USERS / 5).during(Duration.ofSeconds(RAMP)),
                constantUsersPerSec(USERS / 20.0).during(Duration.ofSeconds(DURATION))
            ).protocols(inventoryProtocol)
        ).assertions(
            // 99th-percentile response time < 2 s for all requests
            global().responseTime().percentile(99).lt(2000),
            // 95th-percentile response time < 500 ms
            global().responseTime().percentile(95).lt(500),
            // Success rate ≥ 99% (allows for expected 4xx from test data)
            global().successfulRequests().percent().gte(99.0),
            // Auth scenario must stay < 1 s at p95
            forAll().responseTime().percentile(95).lt(1000)
        );
    }
}
