package com.smartstock.inventory.smoke;

import com.smartstock.inventory.AbstractIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("smoke")
class InventoryServiceSmokeTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    @Test
    void healthEndpoint_returnsUp() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/v1/actuator/health", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"UP\"");
    }

    @Test
    void stockEndpoint_requiresAuth() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/v1/inventory/stock/prod-001/WH-001", String.class);
        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    void openApiDocs_areAccessible() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/v1/v3/api-docs", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
