package com.smartstock.sales.integration;

import com.smartstock.sales.AbstractIntegrationTest;
import com.smartstock.sales.domain.repository.SalesOrderRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SalesOrderController Integration Tests")
class SalesOrderControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() {
        salesOrderRepository.deleteAll();
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(buildToken("user-test", List.of(
                "sales-order:create", "sales-order:read", "sales-order:confirm",
                "sales-order:pick", "sales-order:ship", "sales-order:deliver", "sales-order:write"
        )));
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("POST /sales-orders - creates a sales order")
    void createSalesOrder_returns201() {
        String body = """
                {
                  "customerId": "cust-001",
                  "soNumber": "SO-ITEST-001",
                  "orderDate": "2026-06-20",
                  "dueDate": "2026-06-30",
                  "items": [
                    {
                      "productId": "prod-001",
                      "quantity": 50,
                      "unitPrice": 99.99
                    }
                  ],
                  "shippingAddress": "123 Test St, Newark NJ",
                  "paymentTerms": "NET_30"
                }
                """;

        HttpEntity<String> entity = new HttpEntity<>(body, authHeaders);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/sales-orders", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("soNumber")).isEqualTo("SO-ITEST-001");
        assertThat(data.get("status")).isEqualTo("CREATED");
        assertThat(data.get("customerId")).isEqualTo("cust-001");
    }

    @Test
    @DisplayName("POST /sales-orders - rejects duplicate SO number")
    void createSalesOrder_duplicateNumber_returns409() {
        String body = """
                {
                  "customerId": "cust-001",
                  "soNumber": "SO-ITEST-DUP",
                  "items": [
                    {"productId": "prod-001", "quantity": 10, "unitPrice": 50.00}
                  ]
                }
                """;
        HttpEntity<String> entity = new HttpEntity<>(body, authHeaders);

        restTemplate.postForEntity("/api/v1/sales-orders", entity, Map.class);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/sales-orders", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("GET /sales-orders/{soId} - returns 404 for missing order")
    void getSalesOrder_notFound_returns404() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/sales-orders/non-existent-id",
                HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GET /sales-orders - returns paged list")
    void listSalesOrders_returnsList() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/sales-orders?page=0&size=20",
                HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("POST /sales-orders/{soId}/confirm - confirms CREATED order")
    void confirmSalesOrder_success() {
        String soId = createAndGetSoId("SO-ITEST-CONF");

        String confirmBody = """
                {
                  "warehouseId": "W01",
                  "notes": "Confirmed by test"
                }
                """;
        HttpEntity<String> entity = new HttpEntity<>(confirmBody, authHeaders);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/sales-orders/" + soId + "/confirm", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("CONFIRMED");
        assertThat(data.get("pickingWarehouseId")).isEqualTo("W01");
    }

    @Test
    @DisplayName("POST /sales-orders/{soId}/cancel - cancels CREATED order")
    void cancelSalesOrder_success() {
        String soId = createAndGetSoId("SO-ITEST-CANCEL");

        String cancelBody = """
                {
                  "reason": "Customer changed mind"
                }
                """;
        HttpEntity<String> entity = new HttpEntity<>(cancelBody, authHeaders);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/sales-orders/" + soId + "/cancel", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("CANCELLED");
        assertThat(data.get("cancellationReason")).isEqualTo("Customer changed mind");
    }

    private String createAndGetSoId(String soNumber) {
        String body = String.format("""
                {
                  "customerId": "cust-001",
                  "soNumber": "%s",
                  "items": [
                    {"productId": "prod-001", "quantity": 20, "unitPrice": 50.00}
                  ]
                }
                """, soNumber);
        HttpEntity<String> entity = new HttpEntity<>(body, authHeaders);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/sales-orders", entity, Map.class);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        return (String) data.get("soId");
    }

    private String buildToken(String userId, List<String> permissions) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("email", userId + "@test.com")
                .claim("permissions", permissions)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }
}
