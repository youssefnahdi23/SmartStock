package com.smartstock.identity.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private String adminToken;
    private static final String ADMIN_PASSWORD = "Admin@SmartStock2026!";

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";

        adminToken = given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"system.admin\", \"password\": \"" + ADMIN_PASSWORD + "\"}")
        .when()
            .post("/identity/auth/login")
        .then()
            .statusCode(200)
            .extract().path("data.accessToken");
    }

    @Test
    void register_withValidData_shouldReturn201() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "username": "new.test.user",
                    "email": "new.test@example.com",
                    "password": "SecurePass@2026!",
                    "firstName": "New",
                    "lastName": "User"
                }
                """)
        .when()
            .post("/identity/users/register")
        .then()
            .statusCode(201)
            .body("data.username",  equalTo("new.test.user"))
            .body("data.email",     equalTo("new.test@example.com"))
            .body("data.active",    equalTo(true))
            .body("data.id",        notNullValue());
    }

    @Test
    void register_withWeakPassword_shouldReturn422() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "username": "weakpass.user",
                    "email": "weakpass@example.com",
                    "password": "short",
                    "firstName": "Weak",
                    "lastName": "Pass"
                }
                """)
        .when()
            .post("/identity/users/register")
        .then()
            .statusCode(422)
            .body("errors[0].code", equalTo("VALIDATION_FAILED"));
    }

    @Test
    void register_withDuplicateUsername_shouldReturn400() {
        // system.admin already exists from seed data
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "username": "system.admin",
                    "email": "another@example.com",
                    "password": "SecurePass@2026!",
                    "firstName": "Another",
                    "lastName": "Admin"
                }
                """)
        .when()
            .post("/identity/users/register")
        .then()
            .statusCode(400)
            .body("errors[0].code", equalTo("USER_ALREADY_EXISTS"));
    }

    @Test
    void getUser_asAdmin_shouldReturnUserProfile() {
        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/identity/users/user-system-admin")
        .then()
            .statusCode(200)
            .body("data.id",       equalTo("user-system-admin"))
            .body("data.username", equalTo("system.admin"))
            .body("data.roles",    hasItem("SYSTEM_ADMIN"));
    }

    @Test
    void getUser_withoutAuth_shouldReturn401() {
        given()
        .when()
            .get("/identity/users/user-system-admin")
        .then()
            .statusCode(401);
    }

    @Test
    void listUsers_asAdmin_shouldReturnPaginatedUsers() {
        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/identity/users?page=0&size=10")
        .then()
            .statusCode(200)
            .body("data",          notNullValue())
            .body("meta.page",     equalTo(0))
            .body("meta.size",     equalTo(10))
            .body("meta.total",    greaterThanOrEqualTo(1));
    }

    @Test
    void listRoles_authenticated_shouldReturnRoles() {
        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/identity/roles")
        .then()
            .statusCode(200)
            .body("data",         notNullValue())
            .body("meta.total",   greaterThanOrEqualTo(6));
    }

    @Test
    void listPermissions_authenticated_shouldReturnPermissions() {
        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/identity/permissions")
        .then()
            .statusCode(200)
            .body("data", hasSize(greaterThanOrEqualTo(10)));
    }
}
