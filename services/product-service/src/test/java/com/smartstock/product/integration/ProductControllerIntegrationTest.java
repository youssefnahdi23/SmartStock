package com.smartstock.product.integration;

import com.smartstock.product.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Product Controller integration tests")
class ProductControllerIntegrationTest extends AbstractIntegrationTest {

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
    @DisplayName("GET /products without token — returns 403 or 401")
    void listProducts_withoutToken_returns401Or403() {
        ResponseEntity<String> response = restTemplate.getForEntity("/products", String.class);
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    @DisplayName("GET /products/categories without token — returns 403 or 401")
    void listCategories_withoutToken_returns401Or403() {
        ResponseEntity<String> response = restTemplate.getForEntity("/products/categories", String.class);
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }
}
