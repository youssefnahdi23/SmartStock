package com.smartstock.inventory.integration;

import com.smartstock.inventory.AbstractIntegrationTest;
import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.repository.InventoryLevelRepository;
import com.smartstock.inventory.event.payload.PurchaseOrderEventPayload;
import com.smartstock.inventory.service.PurchaseOrderEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the DeliveryRegisteredEvent → stock-in consumer pipeline end-to-end:
 * event payload → idempotency claim → InventoryService.receiveStockInternal → DB row.
 *
 * Excluded from unit-test phase (surefire *IntegrationTest pattern); runs under
 * the integration-test failsafe profile with Docker/Testcontainers.
 */
@DisplayName("PurchaseOrderEvent → inventory stock-in (end-to-end)")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PurchaseOrderEventIntegrationTest extends AbstractIntegrationTest {

    @Autowired PurchaseOrderEventListener listener;
    @Autowired InventoryLevelRepository   inventoryLevelRepository;
    @Autowired JdbcTemplate               jdbc;

    private static final String PRODUCT_ID   = "PROD-TEST-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String WAREHOUSE_ID = "WH-TEST-1";
    private static final String SUPPLIER_ID  = "SUP-001";

    @BeforeEach
    void setUp() {
        // Ensure a clean inventory level for the test product
        inventoryLevelRepository.findByProductIdAndWarehouseId(PRODUCT_ID, WAREHOUSE_ID)
                .ifPresent(inventoryLevelRepository::delete);
    }

    @Test
    @DisplayName("DeliveryRegisteredEvent creates stock-in and increases on-hand quantity")
    void deliveryRegisteredCreatesStockIn() {
        String eventId    = UUID.randomUUID().toString();
        String deliveryId = UUID.randomUUID().toString();

        PurchaseOrderEventPayload payload = new PurchaseOrderEventPayload(
                eventId, "DeliveryRegisteredEvent", 1, UUID.randomUUID().toString(),
                "PO-001", SUPPLIER_ID, deliveryId, WAREHOUSE_ID,
                LocalDate.now(), "system",
                List.of(new PurchaseOrderEventPayload.ReceivedItem(PRODUCT_ID, 50, 0, null)));

        listener.onPurchaseOrderEvent(payload);

        Optional<InventoryLevel> level =
                inventoryLevelRepository.findByProductIdAndWarehouseId(PRODUCT_ID, WAREHOUSE_ID);
        assertThat(level).isPresent();
        assertThat(level.get().getQuantityOnHand()).isEqualTo(50);
    }

    @Test
    @DisplayName("Second DeliveryRegisteredEvent accumulates on-hand quantity")
    void secondDeliveryAccumulatesStock() {
        String eventId1   = UUID.randomUUID().toString();
        String eventId2   = UUID.randomUUID().toString();
        String deliveryId = UUID.randomUUID().toString();

        listener.onPurchaseOrderEvent(new PurchaseOrderEventPayload(
                eventId1, "DeliveryRegisteredEvent", 1, UUID.randomUUID().toString(),
                "PO-001", SUPPLIER_ID, deliveryId, WAREHOUSE_ID, LocalDate.now(), "system",
                List.of(new PurchaseOrderEventPayload.ReceivedItem(PRODUCT_ID, 30, 0, null))));

        listener.onPurchaseOrderEvent(new PurchaseOrderEventPayload(
                eventId2, "DeliveryRegisteredEvent", 1, UUID.randomUUID().toString(),
                "PO-002", SUPPLIER_ID, deliveryId + "-2", WAREHOUSE_ID, LocalDate.now(), "system",
                List.of(new PurchaseOrderEventPayload.ReceivedItem(PRODUCT_ID, 20, 0, null))));

        InventoryLevel level = inventoryLevelRepository
                .findByProductIdAndWarehouseId(PRODUCT_ID, WAREHOUSE_ID).orElseThrow();
        assertThat(level.getQuantityOnHand()).isEqualTo(50);
    }

    @Test
    @DisplayName("Redelivered DeliveryRegisteredEvent is idempotent — stock counted once (H-3)")
    void redeliveryDoesNotDoubleCountStock() {
        String eventId    = UUID.randomUUID().toString();
        String deliveryId = UUID.randomUUID().toString();

        PurchaseOrderEventPayload payload = new PurchaseOrderEventPayload(
                eventId, "DeliveryRegisteredEvent", 1, UUID.randomUUID().toString(),
                "PO-001", SUPPLIER_ID, deliveryId, WAREHOUSE_ID, LocalDate.now(), "system",
                List.of(new PurchaseOrderEventPayload.ReceivedItem(PRODUCT_ID, 40, 0, null)));

        listener.onPurchaseOrderEvent(payload);
        listener.onPurchaseOrderEvent(payload); // simulated redelivery

        InventoryLevel level = inventoryLevelRepository
                .findByProductIdAndWarehouseId(PRODUCT_ID, WAREHOUSE_ID).orElseThrow();
        assertThat(level.getQuantityOnHand())
                .as("Redelivery must not double-count stock (H-3 idempotency)")
                .isEqualTo(40);
    }

    @Test
    @DisplayName("Non-delivery event types do not create stock movements")
    void nonDeliveryEventsAreIgnored() {
        listener.onPurchaseOrderEvent(new PurchaseOrderEventPayload(
                UUID.randomUUID().toString(), "PurchaseOrderCreatedEvent", 1,
                UUID.randomUUID().toString(), "PO-001", SUPPLIER_ID, null, null,
                null, null, null));

        Optional<InventoryLevel> level =
                inventoryLevelRepository.findByProductIdAndWarehouseId(PRODUCT_ID, WAREHOUSE_ID);
        assertThat(level).isNotPresent();
    }

    @Test
    @DisplayName("Processed event is recorded in the idempotency ledger")
    void processedEventIsRecordedInLedger() {
        String eventId    = UUID.randomUUID().toString();
        String deliveryId = UUID.randomUUID().toString();

        listener.onPurchaseOrderEvent(new PurchaseOrderEventPayload(
                eventId, "DeliveryRegisteredEvent", 1, UUID.randomUUID().toString(),
                "PO-001", SUPPLIER_ID, deliveryId, WAREHOUSE_ID, LocalDate.now(), "system",
                List.of(new PurchaseOrderEventPayload.ReceivedItem(PRODUCT_ID, 10, 0, null))));

        String claimKey = eventId + ":" + PRODUCT_ID;
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM processed_events WHERE event_id = ?", Long.class, claimKey);
        assertThat(count).isEqualTo(1L);
    }
}
