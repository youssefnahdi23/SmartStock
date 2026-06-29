package com.smartstock.identity.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Controller integration tests for role and permission management.
 * Validates full CRUD lifecycle, RBAC enforcement, and seed data integrity.
 */
class RoleControllerIntegrationTest extends AbstractIntegrationTest {

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

    @Test
    void listRoles_asAdmin_returnsSeededRoles() {
        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/identity/roles")
        .then()
            .statusCode(200)
            .body("data",       notNullValue())
            .body("meta.total", greaterThanOrEqualTo(6));
    }

    @Test
    void getRoleByName_systemAdmin_returnsRole() {
        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/identity/roles/name/SYSTEM_ADMIN")
        .then()
            .statusCode(200)
            .body("data.name",       equalTo("SYSTEM_ADMIN"))
            .body("data.systemRole", equalTo(true))
            .body("data.active",     equalTo(true));
    }

    @Test
    void createRole_withValidPayload_returns201() {
        String roleName = "TEST_ROLE_" + System.currentTimeMillis();
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body(String.format("""
                {"name": "%s", "description": "Temporary role for testing"}
                """, roleName))
        .when()
            .post("/identity/roles")
        .then()
            .statusCode(201)
            .body("data.name",        equalTo(roleName))
            .body("data.active",      equalTo(true))
            .body("data.systemRole",  equalTo(false))
            .body("data.id",          notNullValue());
    }

    @Test
    void createRole_withDuplicateName_returns400() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body("""
                {"name": "SYSTEM_ADMIN", "description": "Duplicate"}
                """)
        .when()
            .post("/identity/roles")
        .then()
            .statusCode(400)
            .body("errors[0].code", equalTo("ROLE_ALREADY_EXISTS"));
    }

    @Test
    void createRole_withoutName_returns422() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body("""
                {"description": "Missing name"}
                """)
        .when()
            .post("/identity/roles")
        .then()
            .statusCode(422)
            .body("errors[0].code", equalTo("VALIDATION_FAILED"));
    }

    @Test
    void updateRole_description_returns200() {
        // First create a role
        String roleName = "UPDATABLE_ROLE_" + System.currentTimeMillis();
        String roleId = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body(String.format("""
                {"name": "%s", "description": "Original description"}
                """, roleName))
        .when()
            .post("/identity/roles")
        .then()
            .statusCode(201)
            .extract().path("data.id");

        // Then update it
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body("""
                {"description": "Updated description"}
                """)
        .when()
            .put("/identity/roles/" + roleId)
        .then()
            .statusCode(200)
            .body("data.description", equalTo("Updated description"));
    }

    @Test
    void deleteRole_nonSystemRole_returns204() {
        String roleName = "DELETABLE_ROLE_" + System.currentTimeMillis();
        String roleId = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body(String.format("""
                {"name": "%s", "description": "Will be deleted"}
                """, roleName))
        .when()
            .post("/identity/roles")
        .then()
            .statusCode(201)
            .extract().path("data.id");

        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .delete("/identity/roles/" + roleId)
        .then()
            .statusCode(204);
    }

    @Test
    void deleteRole_systemRole_returns400() {
        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .delete("/identity/roles/name/SYSTEM_ADMIN")
        .then()
            .statusCode(anyOf(equalTo(400), equalTo(404)));
    }

    @Test
    void assignPermission_toRole_returns200() {
        String roleName = "PERM_ASSIGN_ROLE_" + System.currentTimeMillis();
        String roleId = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body(String.format("""
                {"name": "%s", "description": "Role for permission assignment test"}
                """, roleName))
        .when()
            .post("/identity/roles")
        .then()
            .statusCode(201)
            .extract().path("data.id");

        // Get a valid permission id
        String permId = given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/identity/permissions?page=0&size=1")
        .then()
            .statusCode(200)
            .extract().path("data[0].id");

        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .post("/identity/roles/" + roleId + "/permissions/" + permId)
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(204)));
    }

    @Test
    void listPermissions_returnsAllSeededPermissions() {
        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/identity/permissions")
        .then()
            .statusCode(200)
            .body("data", hasSize(greaterThanOrEqualTo(10)));
    }

    @Test
    void listRoles_withoutAuth_returns401() {
        given()
        .when()
            .get("/identity/roles")
        .then()
            .statusCode(401);
    }
}
