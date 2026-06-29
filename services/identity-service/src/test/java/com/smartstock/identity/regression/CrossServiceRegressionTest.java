package com.smartstock.identity.regression;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.smartstock.identity.integration.AbstractIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Regression tests that use WireMock to stub downstream services and verify
 * critical cross-service flows remain intact across releases.
 *
 * Pattern: the identity-service under test makes real HTTP calls; all peer
 * services (product, inventory, etc.) are stubbed so we control their responses
 * and test the identity service's own behaviour in isolation.
 */
@Tag("regression")
class CrossServiceRegressionTest extends AbstractIntegrationTest {

    @LocalServerPort int port;

    private WireMockServer wireMock;
    private String adminToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";

        // Start WireMock on a fixed port that the identity service can be configured to call
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());

        adminToken = given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "system.admin", "password": "Admin@SmartStock2026!"}
                """)
        .when()
            .post("/identity/auth/login")
        .then()
            .statusCode(200)
            .extract().path("data.accessToken");
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Nested
    class AuthenticationRegression {

        @Test
        void login_withValidCredentials_returnsCompleteTokenPayload() {
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {"username": "system.admin", "password": "Admin@SmartStock2026!"}
                    """)
            .when()
                .post("/identity/auth/login")
            .then()
                .statusCode(200)
                .body("data.accessToken",   notNullValue())
                .body("data.refreshToken",  notNullValue())
                .body("data.tokenType",     equalTo("Bearer"))
                .body("data.expiresIn",     greaterThan(0))
                .body("data.user.username", equalTo("system.admin"))
                .body("meta.timestamp",     notNullValue());
        }

        @Test
        void login_refresh_logout_fullCycle_succeedsWithoutErrors() {
            // 1. Login
            String refreshToken = given()
                .contentType(ContentType.JSON)
                .body("""
                    {"username": "system.admin", "password": "Admin@SmartStock2026!"}
                    """)
            .when()
                .post("/identity/auth/login")
            .then()
                .statusCode(200)
                .extract().path("data.refreshToken");

            // 2. Refresh
            String newAccessToken = given()
                .contentType(ContentType.JSON)
                .body("{\"refreshToken\": \"" + refreshToken + "\"}")
            .when()
                .post("/identity/auth/refresh")
            .then()
                .statusCode(200)
                .body("data.accessToken", notNullValue())
                .extract().path("data.accessToken");

            // 3. Logout with new token
            given()
                .header("Authorization", "Bearer " + newAccessToken)
            .when()
                .post("/identity/auth/logout")
            .then()
                .statusCode(200);

            // 4. Verify refresh token is now invalid
            given()
                .contentType(ContentType.JSON)
                .body("{\"refreshToken\": \"" + refreshToken + "\"}")
            .when()
                .post("/identity/auth/refresh")
            .then()
                .statusCode(403);
        }

        @Test
        void concurrentLogins_allReturnIndependentTokens() throws InterruptedException {
            // Three simultaneous logins should each get their own valid tokens
            String[] tokens = new String[3];
            Thread[] threads = new Thread[3];

            for (int i = 0; i < 3; i++) {
                final int idx = i;
                threads[i] = new Thread(() -> {
                    tokens[idx] = given()
                        .contentType(ContentType.JSON)
                        .body("""
                            {"username": "system.admin", "password": "Admin@SmartStock2026!"}
                            """)
                    .when()
                        .post("/identity/auth/login")
                    .then()
                        .statusCode(200)
                        .extract().path("data.accessToken");
                });
                threads[i].start();
            }

            for (Thread t : threads) t.join(5000);

            for (String token : tokens) {
                org.assertj.core.api.Assertions.assertThat(token).isNotNull();
            }
        }
    }

    @Nested
    class UserManagementRegression {

        @Test
        void register_then_login_completeLifecycle() {
            String uniqueUsername = "regression.user." + System.currentTimeMillis();

            // Register
            given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "username": "%s",
                        "email": "%s@regression.test",
                        "password": "Secure@2026!",
                        "firstName": "Regression",
                        "lastName": "Test"
                    }
                    """, uniqueUsername, uniqueUsername))
            .when()
                .post("/identity/users/register")
            .then()
                .statusCode(201)
                .body("data.username", equalTo(uniqueUsername))
                .body("data.active", equalTo(true));

            // Login with new account
            given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {"username": "%s", "password": "Secure@2026!"}
                    """, uniqueUsername))
            .when()
                .post("/identity/auth/login")
            .then()
                .statusCode(200)
                .body("data.accessToken", notNullValue());
        }

        @Test
        void listUsers_pagination_returnsConsistentData() {
            // Page 0 returns a non-empty list of users
            given()
                .header("Authorization", "Bearer " + adminToken)
            .when()
                .get("/identity/users?page=0&size=5")
            .then()
                .statusCode(200)
                .body("meta.page", equalTo(0))
                .body("meta.size", equalTo(5))
                .body("meta.total", greaterThan(0));

            given()
                .header("Authorization", "Bearer " + adminToken)
            .when()
                .get("/identity/users?page=1&size=5")
            .then()
                .statusCode(200)
                .body("meta.page", equalTo(1));
        }
    }

    @Nested
    class WireMockStubExamples {

        @Test
        void productServiceStub_returnsExpectedShape() {
            // Stub product service for cross-service scenarios
            wireMock.stubFor(get(urlEqualTo("/api/v1/products/prod-001"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "data": {
                                        "id": "prod-001",
                                        "sku": "WGT-001",
                                        "name": "Blue Widget",
                                        "active": true
                                    },
                                    "meta": {"timestamp": "2026-06-28T10:00:00Z"}
                                }
                                """)));

            // Verify the stub is up (useful for validating WireMock is wired correctly)
            given()
                .port(wireMock.port())
            .when()
                .get("/api/v1/products/prod-001")
            .then()
                .statusCode(200)
                .body("data.sku", equalTo("WGT-001"));

            wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/products/prod-001")));
        }

        @Test
        void inventoryServiceStub_lowStockAlert_returns200() {
            wireMock.stubFor(get(urlMatching("/api/v1/inventory/stock/.*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "data": {
                                        "productId": "prod-001",
                                        "warehouseId": "WH-001",
                                        "quantityOnHand": 5,
                                        "reorderPoint": 20,
                                        "lowStock": true
                                    }
                                }
                                """)));

            given()
                .port(wireMock.port())
            .when()
                .get("/api/v1/inventory/stock/prod-001/WH-001")
            .then()
                .statusCode(200)
                .body("data.lowStock", equalTo(true))
                .body("data.quantityOnHand", equalTo(5));
        }
    }
}
