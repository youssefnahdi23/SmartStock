package com.smartstock.inventory.unit;

import com.smartstock.inventory.api.dto.request.ReservationRequest;
import com.smartstock.inventory.api.dto.response.ReservationResponse;
import com.smartstock.inventory.domain.model.InventoryHold;
import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.repository.InventoryHoldRepository;
import com.smartstock.inventory.domain.repository.InventoryLevelRepository;
import com.smartstock.inventory.exception.InsufficientStockException;
import com.smartstock.inventory.exception.InventoryLevelNotFoundException;
import com.smartstock.inventory.service.ConcurrencyRetry;
import com.smartstock.inventory.service.InventoryEventPublisher;
import com.smartstock.inventory.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceUnitTest {

    @Mock InventoryLevelRepository inventoryLevelRepository;
    @Mock InventoryHoldRepository inventoryHoldRepository;
    @Mock InventoryEventPublisher eventPublisher;
    @Spy ConcurrencyRetry concurrencyRetry = new ConcurrencyRetry();
    @Mock ObjectProvider<ReservationService> self;

    @InjectMocks ReservationService reservationService;

    private InventoryLevel level;

    @BeforeEach
    void setUp() {
        lenient().when(self.getObject()).thenReturn(reservationService);
        level = InventoryLevel.builder()
                .id("lvl-001")
                .productId("prod-001")
                .warehouseId("wh-001")
                .quantityOnHand(100)
                .quantityReserved(10)
                .unitCost(BigDecimal.valueOf(45.00))
                .build();
    }

    @Test
    void reserve_happyPath_createsHoldAndReducesAvailableStock() {
        ReservationRequest req = new ReservationRequest();
        req.setProductId("prod-001");
        req.setWarehouseId("wh-001");
        req.setQuantity(20);
        req.setOrderId("order-001");
        req.setReservationReason("SALES_ORDER");

        InventoryHold savedHold = InventoryHold.builder()
                .id("hold-001")
                .productId("prod-001")
                .warehouseId("wh-001")
                .quantityHeld(20)
                .holdReason("SALES_ORDER")
                .orderId("order-001")
                .heldAt(Instant.now())
                .heldBy("user-001")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();

        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-001", "wh-001"))
                .thenReturn(Optional.of(level));
        when(inventoryLevelRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(inventoryHoldRepository.save(any())).thenReturn(savedHold);

        ReservationResponse result = reservationService.reserve(req, "user-001");

        assertThat(result.getReservationId()).isEqualTo("hold-001");
        assertThat(result.getProductId()).isEqualTo("prod-001");
        assertThat(result.getWarehouseId()).isEqualTo("wh-001");
        assertThat(result.getQuantity()).isEqualTo(20);
        assertThat(result.getOrderId()).isEqualTo("order-001");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        // available was 90 (100 on-hand - 10 reserved); after reserving 20 it becomes 70
        assertThat(level.getQuantityReserved()).isEqualTo(30);

        verify(inventoryLevelRepository).save(any());
        verify(inventoryHoldRepository).save(any());
        verify(eventPublisher).publishStockReserved(any());
    }

    @Test
    void reserve_insufficientStock_throwsInsufficientStockException() {
        ReservationRequest req = new ReservationRequest();
        req.setProductId("prod-001");
        req.setWarehouseId("wh-001");
        req.setQuantity(500); // available is only 90
        req.setReservationReason("SALES_ORDER");

        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-001", "wh-001"))
                .thenReturn(Optional.of(level));

        assertThatThrownBy(() -> reservationService.reserve(req, "user-001"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        verify(inventoryLevelRepository, never()).save(any());
        verify(inventoryHoldRepository, never()).save(any());
        verify(eventPublisher, never()).publishStockReserved(any());
    }

    @Test
    void reserve_inventoryLevelNotFound_throwsException() {
        ReservationRequest req = new ReservationRequest();
        req.setProductId("prod-999");
        req.setWarehouseId("wh-999");
        req.setQuantity(10);

        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-999", "wh-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.reserve(req, "user-001"))
                .isInstanceOf(InventoryLevelNotFoundException.class);

        verify(inventoryHoldRepository, never()).save(any());
        verify(eventPublisher, never()).publishStockReserved(any());
    }
}
