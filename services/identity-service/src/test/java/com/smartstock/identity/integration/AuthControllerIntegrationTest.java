package com.smartstock.identity.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    @Test
    void login_withValidAdminCredentials_shouldReturn200WithTokens() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "system.admin", "password": "Admin@SmartStock2026!"}
                """)
        .when()
            .post("/identity/auth/login")
        .then()
            .statusCode(200)
            .body("data.accessToken",  notNullValue())
            .body("data.refreshToken", notNullValue())
            .body("data.tokenType",    equalTo("Bearer"))
            .body("data.expiresIn",    greaterThan(0))
            .body("data.user.username", equalTo("system.admin"))
            .body("meta.timestamp",    notNullValue());
    }

    @Test
    void login_withWrongPassword_shouldReturn401() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "system.admin", "password": "WrongPassword!"}
                """)
        .when()
            .post("/identity/auth/login")
        .then()
            .statusCode(401)
            .body("errors[0].code", equalTo("INVALID_CREDENTIALS"));
    }

    @Test
    void login_withMissingFields_shouldReturn422() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/identity/auth/login")
        .then()
            .statusCode(422)
            .body("errors", hasSize(greaterThan(0)))
            .body("errors[0].code", equalTo("VALIDATION_FAILED"));
    }

    @Test
    void login_withUnknownUser_shouldReturn401() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "nonexistent.user", "password": "Password123!"}
                """)
        .when()
            .post("/identity/auth/login")
        .then()
            .statusCode(401)
            .body("errors[0].code", equalTo("INVALID_CREDENTIALS"));
    }

    @Test
    void refresh_withInvalidToken_shouldReturn403() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"refreshToken": "invalid-token"}
                """)
        .when()
            .post("/identity/auth/refresh")
        .then()
            .statusCode(403)
            .body("errors[0].code", equalTo("REFRESH_TOKEN_REVOKED"));
    }

    @Test
    void logout_withoutAuthToken_shouldReturn401() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/identity/auth/logout")
        .then()
            .statusCode(401);
    }

    @Test
    void forgotPassword_withUnknownEmail_shouldReturn404() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"email": "nobody@nowhere.com"}
                """)
        .when()
            .post("/identity/auth/forgot-password")
        .then()
            .statusCode(404)
            .body("errors[0].code", equalTo("USER_NOT_FOUND"));
    }

    @Test
    void fullLoginRefreshLogoutFlow_shouldSucceed() {
        // Login
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

        // Refresh
        String newAccessToken = given()
            .contentType(ContentType.JSON)
            .body("{\"refreshToken\": \"" + refreshToken + "\"}")
        .when()
            .post("/identity/auth/refresh")
        .then()
            .statusCode(200)
            .body("data.accessToken", notNullValue())
            .extract().path("data.accessToken");

        // Logout
        given()
            .header("Authorization", "Bearer " + newAccessToken)
        .when()
            .post("/identity/auth/logout")
        .then()
            .statusCode(200)
            .body("data.message", equalTo("Logout successful"));
    }
}
