package com.smartstock.identity.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Security integration tests: expired JWT, RBAC violations, CORS headers,
 * SQL injection guards, brute-force lockout behaviour.
 */
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private String adminToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";

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

    @Nested
    class JwtTokenValidation {

        @Test
        void request_withMalformedToken_returns401() {
            given()
                .header("Authorization", "Bearer not.a.real.jwt")
            .when()
                .get("/identity/users")
            .then()
                .statusCode(401);
        }

        @Test
        void request_withWrongSignatureToken_returns401() {
            // Signed with a different secret
            String wrongToken = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJzdWIiOiJ1c2VyLTAwMSIsInJvbGVzIjpbIlJPTEVfQURNSU4iXX0"
                + ".invalid-signature-here";

            given()
                .header("Authorization", "Bearer " + wrongToken)
            .when()
                .get("/identity/users")
            .then()
                .statusCode(401);
        }

        @Test
        void request_withoutBearerPrefix_returns401() {
            given()
                .header("Authorization", adminToken)
            .when()
                .get("/identity/users")
            .then()
                .statusCode(401);
        }

        @Test
        void request_withValidToken_returns200() {
            given()
                .header("Authorization", "Bearer " + adminToken)
            .when()
                .get("/identity/users?page=0&size=5")
            .then()
                .statusCode(200);
        }
    }

    @Nested
    class RbacViolations {

        private String lowPrivToken;

        @BeforeEach
        void registerLowPrivUser() {
            // Register a plain user (ROLE_USER, no admin permissions)
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "username": "rbac.test.user",
                        "email": "rbac.test@example.com",
                        "password": "SecurePass@2026!",
                        "firstName": "Rbac",
                        "lastName": "Test"
                    }
                    """)
            .when()
                .post("/identity/users/register");

            lowPrivToken = given()
                .contentType(ContentType.JSON)
                .body("""
                    {"username": "rbac.test.user", "password": "SecurePass@2026!"}
                    """)
            .when()
                .post("/identity/auth/login")
            .then()
                .statusCode(200)
                .extract().path("data.accessToken");
        }

        @Test
        void listUsers_asPlainUser_returns403() {
            given()
                .header("Authorization", "Bearer " + lowPrivToken)
            .when()
                .get("/identity/users")
            .then()
                .statusCode(403);
        }

        @Test
        void createRole_asPlainUser_returns403() {
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + lowPrivToken)
                .body("""
                    {"name": "FAKE_ROLE", "description": "Should be rejected"}
                    """)
            .when()
                .post("/identity/roles")
            .then()
                .statusCode(403);
        }

        @Test
        void listPermissions_asPlainUser_returns403() {
            given()
                .header("Authorization", "Bearer " + lowPrivToken)
            .when()
                .get("/identity/permissions")
            .then()
                .statusCode(403);
        }

        @Test
        void deactivateUser_asPlainUser_returns403() {
            given()
                .header("Authorization", "Bearer " + lowPrivToken)
            .when()
                .patch("/identity/users/user-system-admin/deactivate")
            .then()
                .statusCode(403);
        }
    }

    @Nested
    class InputSanitisation {

        @Test
        void login_withSqlInjectionInUsername_returns401NotServerError() {
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {"username": "' OR '1'='1", "password": "password"}
                    """)
            .when()
                .post("/identity/auth/login")
            .then()
                .statusCode(401)
                .body("errors[0].code", equalTo("INVALID_CREDENTIALS"));
        }

        @Test
        void login_withXssInUsername_returns401NotServerError() {
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {"username": "<script>alert(1)</script>", "password": "pw"}
                    """)
            .when()
                .post("/identity/auth/login")
            .then()
                .statusCode(401);
        }

        @Test
        void register_withOversizedField_returns422() {
            String huge = "a".repeat(300);
            given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {"username": "%s", "email": "x@x.com",
                     "password": "Valid@2026!", "firstName": "X", "lastName": "Y"}
                    """, huge))
            .when()
                .post("/identity/users/register")
            .then()
                .statusCode(422);
        }
    }

    @Nested
    class BruteForce {

        @Test
        void login_fiveWrongPasswords_locksAccount() {
            // Register a fresh user for lockout test
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "username": "lockout.user",
                        "email": "lockout@example.com",
                        "password": "Correct@2026!",
                        "firstName": "Lock",
                        "lastName": "Out"
                    }
                    """)
            .when()
                .post("/identity/users/register")
            .then()
                .statusCode(201);

            // 5 failed attempts
            for (int i = 0; i < 5; i++) {
                given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {"username": "lockout.user", "password": "WrongPassword!"}
                        """)
                .when()
                    .post("/identity/auth/login")
                .then()
                    .statusCode(401);
            }

            // 6th attempt should see locked account
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {"username": "lockout.user", "password": "Correct@2026!"}
                    """)
            .when()
                .post("/identity/auth/login")
            .then()
                .statusCode(423)
                .body("errors[0].code", equalTo("ACCOUNT_LOCKED"));
        }
    }

    @Nested
    class CorsHeaders {

        @Test
        void preflight_fromAllowedOrigin_returnsOk() {
            given()
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization")
            .when()
                .options("/identity/auth/login")
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)))
                .header("Access-Control-Allow-Origin", notNullValue());
        }
    }

    @Nested
    class ActuatorEndpoints {

        @Test
        void healthEndpoint_isPubliclyAccessible() {
            given()
            .when()
                .get("/actuator/health")
            .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
        }

        @Test
        void actuatorEnv_isNotPubliclyAccessible() {
            given()
            .when()
                .get("/actuator/env")
            .then()
                .statusCode(anyOf(equalTo(401), equalTo(403), equalTo(404)));
        }
    }
}
