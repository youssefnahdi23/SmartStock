package com.smartstock.customer.repository;

import com.smartstock.customer.domain.model.Customer;
import com.smartstock.customer.domain.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null")
class CustomerRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("smartstock_customer_test")
                    .withUsername("smartstock")
                    .withPassword("smartstock");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url",          postgres::getJdbcUrl);
        registry.add("spring.flyway.user",         postgres::getUsername);
        registry.add("spring.flyway.password",     postgres::getPassword);
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired private CustomerRepository customerRepository;

    private Customer premiumCustomer;
    private Customer standardCustomer;

    @BeforeEach
    void seed() {
        customerRepository.deleteAll();

        premiumCustomer = customerRepository.save(Customer.builder()
                .customerCode("CUST-REPO-001")
                .customerName("Acme Corp")
                .segment("PREMIUM")
                .isActive(true)
                .createdBy("seed")
                .updatedBy("seed")
                .build());

        standardCustomer = customerRepository.save(Customer.builder()
                .customerCode("CUST-REPO-002")
                .customerName("Beta Ltd")
                .segment("STANDARD")
                .isActive(true)
                .createdBy("seed")
                .updatedBy("seed")
                .build());
    }

    @Test
    void existsByCustomerCode_existingCode_returnsTrue() {
        assertThat(customerRepository.existsByCustomerCode("CUST-REPO-001")).isTrue();
    }

    @Test
    void existsByCustomerCode_unknownCode_returnsFalse() {
        assertThat(customerRepository.existsByCustomerCode("NONEXISTENT")).isFalse();
    }

    @Test
    void findByCustomerCode_existingCode_returnsCustomer() {
        Optional<Customer> result = customerRepository.findByCustomerCode("CUST-REPO-001");
        assertThat(result).isPresent();
        assertThat(result.get().getCustomerName()).isEqualTo("Acme Corp");
    }

    @Test
    void findByCustomerCode_unknownCode_returnsEmpty() {
        assertThat(customerRepository.findByCustomerCode("NO-SUCH-CODE")).isEmpty();
    }

    @Nested
    class FindWithFilters {

        @Test
        void noFilters_returnsAll() {
            Page<Customer> page = customerRepository.findWithFilters(
                    null, null, null, null, PageRequest.of(0, 10));
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        void filterByStatus_active_returnsActiveCustomers() {
            Page<Customer> page = customerRepository.findWithFilters(
                    null, null, "ACTIVE", null, PageRequest.of(0, 10));
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        void filterBySegment_returnsPremiumOnly() {
            Page<Customer> page = customerRepository.findWithFilters(
                    null, "PREMIUM", null, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getId()).isEqualTo(premiumCustomer.getId());
        }

        @Test
        void filterBySearch_matchesCustomerName() {
            Page<Customer> page = customerRepository.findWithFilters(
                    null, null, null, "acme", PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getId()).isEqualTo(premiumCustomer.getId());
        }
    }

    @Test
    void findBySegment_premiumSegment_returnsActiveOnly() {
        Page<Customer> page = customerRepository.findBySegment("PREMIUM", PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(c -> c.getSegment().equals("PREMIUM"));
        assertThat(page.getContent()).allMatch(c -> c.getIsActive());
    }

    @Test
    void findBySegment_standardSegment_returnsStandardCustomer() {
        Page<Customer> page = customerRepository.findBySegment("STANDARD", PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(standardCustomer.getId());
    }

    @Test
    void save_autoAssignsId() {
        Customer saved = customerRepository.save(Customer.builder()
                .customerCode("CUST-REPO-003")
                .customerName("Gamma GmbH")
                .segment("STANDARD")
                .isActive(true)
                .createdBy("test")
                .updatedBy("test")
                .build());
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
