package com.smartstock.supplier.integration;

import com.smartstock.supplier.AbstractIntegrationTest;
import com.smartstock.supplier.domain.repository.SupplierDeliveryRepository;
import com.smartstock.supplier.event.payload.PurchaseOrderEventPayload;
import com.smartstock.supplier.service.PurchaseOrderEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the PurchaseOrder DeliveryRegisteredEvent → supplier delivery record pipeline:
 * event payload → idempotency claim → SupplierDeliveryService.registerDeliveryFromPurchaseOrderEvent.
 */
@DisplayName("PurchaseOrderEvent → supplier delivery record (end-to-end)")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PurchaseOrderEventIntegrationTest extends AbstractIntegrationTest {

    @Autowired PurchaseOrderEventListener   listener;
    @Autowired SupplierDeliveryRepository   deliveryRepository;
    @Autowired JdbcTemplate                 jdbc;

    private static final String SUPPLIER_ID = "SUP-" + UUID.randomUUID().toString().substring(0, 8);

    /** Convenience factory — keeps test data from repeating the 17-field canonical constructor. */
    private static PurchaseOrderEventPayload delivery(String eventId, String deliveryId, String supplierId) {
        return new PurchaseOrderEventPayload(
                eventId, "DeliveryRegisteredEvent", UUID.randomUUID().toString(),
                "PO-001", supplierId, deliveryId, "WH-1",
                LocalDate.now(), 100, 0, "system",
                null, null, null, null, null, null);
    }

    private static PurchaseOrderEventPayload qualityIssue(String eventId, String supplierId) {
        return new PurchaseOrderEventPayload(
                eventId, "QualityIssueReportedEvent", UUID.randomUUID().toString(),
                "PO-001", supplierId, null, null,
                null, null, null, null,
                "ISSUE-1", "DAMAGED", 5, "HIGH", "REPLACE", "inspector");
    }

    @Test
    @DisplayName("DeliveryRegisteredEvent creates a SupplierDelivery row")
    void deliveryRegisteredCreatesDeliveryRow() {
        long before = deliveryRepository.count();

        listener.onPurchaseOrderEvent(delivery(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), SUPPLIER_ID));

        assertThat(deliveryRepository.count()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("Redelivered DeliveryRegisteredEvent does not create duplicate delivery rows (H-3)")
    void redeliveryDoesNotDuplicateDeliveryRow() {
        PurchaseOrderEventPayload payload = delivery(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), SUPPLIER_ID);

        listener.onPurchaseOrderEvent(payload);
        long countAfterFirst = deliveryRepository.count();

        listener.onPurchaseOrderEvent(payload); // redelivery
        assertThat(deliveryRepository.count())
                .as("Redelivery must not create a second delivery row (H-3)")
                .isEqualTo(countAfterFirst);
    }

    @Test
    @DisplayName("QualityIssueReportedEvent is consumed without error and claims idempotency ledger")
    void qualityIssueEventIsAcknowledged() {
        String eventId = UUID.randomUUID().toString();

        listener.onPurchaseOrderEvent(qualityIssue(eventId, SUPPLIER_ID)); // must not throw

        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM processed_events WHERE event_id = ?",
                Long.class, eventId);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("Non-delivery PO events are silently ignored")
    void otherPoEventsAreIgnored() {
        long before = deliveryRepository.count();

        listener.onPurchaseOrderEvent(new PurchaseOrderEventPayload(
                UUID.randomUUID().toString(), "PurchaseOrderCreatedEvent",
                UUID.randomUUID().toString(),
                "PO-002", SUPPLIER_ID, null, null,
                null, null, null, null,
                null, null, null, null, null, null));

        assertThat(deliveryRepository.count()).isEqualTo(before);
    }
}
