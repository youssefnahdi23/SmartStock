package com.smartstock.purchase.smoke;

import com.smartstock.purchase.AbstractIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("smoke")
class PurchaseOrderServiceSmokeTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    @Test
    void healthEndpoint_returnsUp() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"UP\"");
    }

    @Test
    void purchaseOrdersEndpoint_requiresAuth() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/purchase-orders", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void openApiDocs_areAccessible() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/v3/api-docs", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
