package com.smartstock.identity.smoke;

import com.smartstock.identity.integration.AbstractIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Smoke tests: verify the service boots and core infrastructure is live.
 * These run in CI after every deployment and must complete in under 30 s.
 */
@Tag("smoke")
class IdentityServiceSmokeTest extends AbstractIntegrationTest {

    @LocalServerPort int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    @Test
    void healthEndpoint_returnsUp() {
        given()
        .when()
            .get("/actuator/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }

    @Test
    void loginEndpoint_isReachable() {
        given()
            .contentType("application/json")
            .body("{}")
        .when()
            .post("/identity/auth/login")
        .then()
            .statusCode(422); // validation fires — endpoint is live
    }

    @Test
    void openApiDocs_areAccessible() {
        given()
        .when()
            .get("/v3/api-docs")
        .then()
            .statusCode(200);
    }
}
