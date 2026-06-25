package com.smartstock.inventory.unit;

import com.smartstock.inventory.api.dto.request.AdjustmentRequest;
import com.smartstock.inventory.api.dto.response.AdjustmentResponse;
import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.model.StockAdjustment;
import com.smartstock.inventory.domain.model.StockMovement;
import com.smartstock.inventory.domain.repository.InventoryLevelRepository;
import com.smartstock.inventory.domain.repository.StockAdjustmentRepository;
import com.smartstock.inventory.domain.repository.StockMovementRepository;
import com.smartstock.inventory.exception.InventoryLevelNotFoundException;
import com.smartstock.inventory.service.AdjustmentService;
import com.smartstock.inventory.service.InventoryEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdjustmentServiceUnitTest {

    @Mock InventoryLevelRepository inventoryLevelRepository;
    @Mock StockMovementRepository stockMovementRepository;
    @Mock StockAdjustmentRepository stockAdjustmentRepository;
    @Mock InventoryEventPublisher eventPublisher;

    @InjectMocks AdjustmentService adjustmentService;

    private InventoryLevel existingLevel;
    private StockMovement savedMovement;
    private StockAdjustment savedAdjustment;

    @BeforeEach
    void setUp() {
        existingLevel = InventoryLevel.builder()
                .id("lvl-001")
                .productId("prod-001")
                .warehouseId("wh-001")
                .quantityOnHand(100)
                .quantityReserved(0)
                .unitCost(BigDecimal.valueOf(45.00))
                .build();

        savedMovement = StockMovement.builder()
                .id("mov-001")
                .movementType("ADJUSTMENT")
                .productId("prod-001")
                .warehouseId("wh-001")
                .quantity(10)
                .previousBalance(100)
                .newBalance(110)
                .actorId("user-001")
                .timestamp(Instant.now())
                .build();

        savedAdjustment = StockAdjustment.builder()
                .id("adj-001")
                .stockMovementId("mov-001")
                .adjustmentType("CORRECTION")
                .adjustmentReason("Recount correction")
                .adjustmentQuantity(10)
                .previousQuantity(100)
                .newQuantity(110)
                .adjustedAt(Instant.now())
                .adjustedBy("user-001")
                .approvalStatus("APPROVED")
                .approvedBy("user-001")
                .approvedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void adjust_happyPath_savesAdjustmentAndUpdatesInventoryLevel() {
        AdjustmentRequest req = new AdjustmentRequest();
        req.setProductId("prod-001");
        req.setWarehouseId("wh-001");
        req.setAdjustmentQuantity(10);
        req.setReason("Recount correction");
        req.setAdjustmentType("CORRECTION");

        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-001", "wh-001"))
                .thenReturn(Optional.of(existingLevel));
        when(inventoryLevelRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any())).thenReturn(savedMovement);
        when(stockAdjustmentRepository.save(any())).thenReturn(savedAdjustment);

        AdjustmentResponse result = adjustmentService.adjust(req, "user-001");

        assertThat(result.getAdjustmentId()).isEqualTo("adj-001");
        assertThat(result.getProductId()).isEqualTo("prod-001");
        assertThat(result.getWarehouseId()).isEqualTo("wh-001");
        assertThat(result.getAdjustmentQuantity()).isEqualTo(10);
        assertThat(result.getPreviousStockLevel()).isEqualTo(100);
        assertThat(result.getNewStockLevel()).isEqualTo(110);
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(existingLevel.getQuantityOnHand()).isEqualTo(110);

        verify(inventoryLevelRepository).save(any());
        verify(stockMovementRepository).save(any());
        verify(stockAdjustmentRepository).save(any());
        verify(eventPublisher).publishStockAdjusted(any());
    }

    @Test
    void adjust_negativeQuantity_reducesStock() {
        AdjustmentRequest req = new AdjustmentRequest();
        req.setProductId("prod-001");
        req.setWarehouseId("wh-001");
        req.setAdjustmentQuantity(-20);
        req.setReason("Shrinkage");
        req.setAdjustmentType("SHRINKAGE");

        StockMovement mvNeg = StockMovement.builder()
                .id("mov-002").movementType("ADJUSTMENT")
                .productId("prod-001").warehouseId("wh-001")
                .quantity(20).previousBalance(100).newBalance(80)
                .actorId("user-001").build();

        StockAdjustment adjNeg = StockAdjustment.builder()
                .id("adj-002").stockMovementId("mov-002")
                .adjustmentType("SHRINKAGE").adjustmentReason("Shrinkage")
                .adjustmentQuantity(-20).previousQuantity(100).newQuantity(80)
                .adjustedAt(Instant.now()).adjustedBy("user-001")
                .approvalStatus("APPROVED").approvedBy("user-001").approvedAt(Instant.now())
                .createdAt(Instant.now()).build();

        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-001", "wh-001"))
                .thenReturn(Optional.of(existingLevel));
        when(inventoryLevelRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any())).thenReturn(mvNeg);
        when(stockAdjustmentRepository.save(any())).thenReturn(adjNeg);

        AdjustmentResponse result = adjustmentService.adjust(req, "user-001");

        assertThat(result.getAdjustmentQuantity()).isEqualTo(-20);
        assertThat(existingLevel.getQuantityOnHand()).isEqualTo(80);
        verify(eventPublisher).publishStockAdjusted(any());
    }

    @Test
    void adjust_inventoryLevelNotFound_throwsException() {
        AdjustmentRequest req = new AdjustmentRequest();
        req.setProductId("prod-999");
        req.setWarehouseId("wh-999");
        req.setAdjustmentQuantity(5);
        req.setReason("Test");

        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-999", "wh-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adjustmentService.adjust(req, "user-001"))
                .isInstanceOf(InventoryLevelNotFoundException.class);

        verify(stockMovementRepository, never()).save(any());
        verify(stockAdjustmentRepository, never()).save(any());
    }
}
