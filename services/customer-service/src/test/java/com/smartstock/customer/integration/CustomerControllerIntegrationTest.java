package com.smartstock.customer.integration;

import com.smartstock.customer.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void actuatorHealth_returnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void listCustomers_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/customers", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCustomer_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/customers/non-existent", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createCustomer_withoutToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {
                  "customerCode": "CUST-TEST-001",
                  "customerName": "Test Customer",
                  "customerType": "RETAIL"
                }
                """;
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/customers", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void suspendCustomer_withoutToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                { "reason": "Test suspension" }
                """;
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/customers/cust-001/suspend", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCustomerContacts_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/customers/cust-001/contacts", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCustomerAddresses_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/customers/cust-001/addresses", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getBySegment_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/customers/by-segment?segment=ENTERPRISE", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
