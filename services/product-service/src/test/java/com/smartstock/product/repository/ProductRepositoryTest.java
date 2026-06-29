package com.smartstock.product.repository;

import com.smartstock.product.domain.model.Product;
import com.smartstock.product.domain.repository.ProductRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("smartstock_product_test")
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

    @Autowired private ProductRepository productRepository;

    private Product widget;
    private Product gadget;

    @BeforeEach
    void seed() {
        productRepository.deleteAll();

        widget = productRepository.save(Product.builder()
                .id("prod-widget-001")
                .sku("WGT-001")
                .name("Blue Widget")
                .description("A reliable blue widget")
                .manufacturer("WidgetCo")
                .brand("WidgetBrand")
                .unitOfMeasure("PIECE")
                .standardCost(BigDecimal.valueOf(10.00))
                .standardRetailPrice(BigDecimal.valueOf(25.00))
                .reorderLevel(5)
                .reorderQuantity(20)
                .active(true)
                .lifecycleStatus("ACTIVE")
                .createdBy("seed")
                .updatedBy("seed")
                .build());

        gadget = productRepository.save(Product.builder()
                .id("prod-gadget-001")
                .sku("GDG-001")
                .name("Red Gadget")
                .description("A fancy red gadget")
                .manufacturer("GadgetCo")
                .brand("GadgetBrand")
                .unitOfMeasure("PIECE")
                .standardCost(BigDecimal.valueOf(50.00))
                .standardRetailPrice(BigDecimal.valueOf(120.00))
                .reorderLevel(3)
                .reorderQuantity(10)
                .active(false)
                .lifecycleStatus("DISCONTINUED")
                .createdBy("seed")
                .updatedBy("seed")
                .build());
    }

    @Nested
    class FindByIdAndNotDeleted {

        @Test
        void existingProduct_returnsProduct() {
            Optional<Product> result = productRepository.findByIdAndNotDeleted("prod-widget-001");
            assertThat(result).isPresent();
            assertThat(result.get().getSku()).isEqualTo("WGT-001");
        }

        @Test
        void softDeletedProduct_returnsEmpty() {
            widget.setDeletedAt(LocalDateTime.now());
            productRepository.save(widget);

            assertThat(productRepository.findByIdAndNotDeleted("prod-widget-001")).isEmpty();
        }

        @Test
        void unknownId_returnsEmpty() {
            assertThat(productRepository.findByIdAndNotDeleted("no-such-id")).isEmpty();
        }
    }

    @Nested
    class FindBySkuAndNotDeleted {

        @Test
        void existingSku_returnsProduct() {
            Optional<Product> result = productRepository.findBySkuAndNotDeleted("WGT-001");
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Blue Widget");
        }

        @Test
        void unknownSku_returnsEmpty() {
            assertThat(productRepository.findBySkuAndNotDeleted("NO-SUCH-SKU")).isEmpty();
        }

        @Test
        void deletedSku_returnsEmpty() {
            widget.setDeletedAt(LocalDateTime.now());
            productRepository.save(widget);

            assertThat(productRepository.findBySkuAndNotDeleted("WGT-001")).isEmpty();
        }
    }

    @Nested
    class ExistsBySku {

        @Test
        void existingActiveSku_returnsTrue() {
            assertThat(productRepository.existsBySkuAndNotDeleted("WGT-001")).isTrue();
        }

        @Test
        void unknownSku_returnsFalse() {
            assertThat(productRepository.existsBySkuAndNotDeleted("X-UNKNOWN")).isFalse();
        }

        @Test
        void existsBySkuExcludingOwnId_doesNotCountItself() {
            assertThat(productRepository.existsBySkuAndNotDeletedExcluding("WGT-001", "prod-widget-001"))
                    .isFalse();
        }

        @Test
        void existsBySkuExcludingOtherProduct_returnsTrueForConflict() {
            assertThat(productRepository.existsBySkuAndNotDeletedExcluding("WGT-001", "prod-gadget-001"))
                    .isTrue();
        }
    }

    @Nested
    class FindAllWithFilters {

        @Test
        void noFilters_returnsAllActive() {
            Page<Product> page = productRepository.findAllWithFilters(
                    null, null, null, PageRequest.of(0, 10));
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        void filterActive_returnsOnlyActiveProducts() {
            Page<Product> page = productRepository.findAllWithFilters(
                    null, true, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).allMatch(Product::isActive);
            assertThat(page.getTotalElements()).isEqualTo(1);
        }

        @Test
        void searchByName_returnMatchingProducts() {
            Page<Product> page = productRepository.findAllWithFilters(
                    "widget", null, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getSku()).isEqualTo("WGT-001");
        }

        @Test
        void searchBySku_returnsMatchingProduct() {
            Page<Product> page = productRepository.findAllWithFilters(
                    "GDG", null, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getName()).isEqualTo("Red Gadget");
        }

        @Test
        void pagination_works() {
            Page<Product> page1 = productRepository.findAllWithFilters(
                    null, null, null, PageRequest.of(0, 1));
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page1.getTotalPages()).isEqualTo(2);
        }
    }

    @Test
    void findById_discontinuedProduct_stillReturnedByJpaFindById() {
        // gadget is DISCONTINUED but not soft-deleted — JPA findById returns it
        assertThat(productRepository.findById(gadget.getId())).isPresent();
        assertThat(productRepository.findById(gadget.getId()).get().getLifecycleStatus())
                .isEqualTo("DISCONTINUED");
    }

    @Test
    void saveAndFlush_setsCreatedAtAndUpdatedAt() {
        Product saved = productRepository.saveAndFlush(Product.builder()
                .id("prod-ts-001")
                .sku("TS-001")
                .name("Timestamp Product")
                .unitOfMeasure("PIECE")
                .standardCost(BigDecimal.ONE)
                .standardRetailPrice(BigDecimal.TEN)
                .createdBy("user")
                .updatedBy("user")
                .build());

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void deactivateAndReactivate_persistsLifecycleStatus() {
        widget.deactivate("user-1");
        productRepository.saveAndFlush(widget);

        Product fetched = productRepository.findById("prod-widget-001").orElseThrow();
        assertThat(fetched.isActive()).isFalse();
        assertThat(fetched.getLifecycleStatus()).isEqualTo("DISCONTINUED");

        fetched.reactivate("user-1");
        productRepository.saveAndFlush(fetched);

        Product reactivated = productRepository.findById("prod-widget-001").orElseThrow();
        assertThat(reactivated.isActive()).isTrue();
        assertThat(reactivated.getLifecycleStatus()).isEqualTo("ACTIVE");
    }
}
