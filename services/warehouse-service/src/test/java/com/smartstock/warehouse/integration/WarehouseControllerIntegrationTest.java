package com.smartstock.warehouse.integration;

import com.smartstock.warehouse.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Warehouse Controller integration tests")
class WarehouseControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("GET /actuator/health — returns 200 UP")
    void healthCheck_returns200() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    @DisplayName("GET /warehouses without token — returns 401 or 403")
    void listWarehouses_withoutToken_returns401Or403() {
        ResponseEntity<String> response = restTemplate.getForEntity("/warehouses", String.class);
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    @DisplayName("POST /warehouses without token — returns 401 or 403")
    void createWarehouse_withoutToken_returns401Or403() {
        ResponseEntity<String> response = restTemplate.postForEntity("/warehouses",
                "{\"code\":\"W01\",\"name\":\"Test\"}", String.class);
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }
}
