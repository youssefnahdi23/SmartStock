package com.smartstock.inventory.unit;

import com.smartstock.inventory.api.dto.request.TransferRequest;
import com.smartstock.inventory.api.dto.response.TransferResponse;
import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.model.StockMovement;
import com.smartstock.inventory.domain.model.StockTransfer;
import com.smartstock.inventory.domain.repository.InventoryLevelRepository;
import com.smartstock.inventory.domain.repository.StockMovementRepository;
import com.smartstock.inventory.domain.repository.StockTransferRepository;
import com.smartstock.inventory.exception.InsufficientStockException;
import com.smartstock.inventory.service.InventoryEventPublisher;
import com.smartstock.inventory.service.TransferService;
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
class TransferServiceUnitTest {

    @Mock InventoryLevelRepository inventoryLevelRepository;
    @Mock StockMovementRepository stockMovementRepository;
    @Mock StockTransferRepository stockTransferRepository;
    @Mock InventoryEventPublisher eventPublisher;

    @InjectMocks TransferService transferService;

    @Test
    void transfer_happyPath_reduceFromAndIncreaseToWarehouse() {
        TransferRequest req = new TransferRequest();
        req.setProductId("prod-001");
        req.setFromWarehouseId("wh-001");
        req.setToWarehouseId("wh-002");
        req.setQuantity(25);
        req.setReason("Rebalancing");

        InventoryLevel fromLevel = InventoryLevel.builder()
                .productId("prod-001").warehouseId("wh-001")
                .quantityOnHand(100).quantityReserved(0)
                .unitCost(BigDecimal.valueOf(45.00)).build();

        InventoryLevel toLevel = InventoryLevel.builder()
                .productId("prod-001").warehouseId("wh-002")
                .quantityOnHand(50).quantityReserved(0)
                .unitCost(BigDecimal.valueOf(45.00)).build();

        StockMovement movement = StockMovement.builder().id("mov-001")
                .movementType("TRANSFER").quantity(25)
                .productId("prod-001").warehouseId("wh-001")
                .actorId("user-001").timestamp(Instant.now()).build();

        StockTransfer transfer = StockTransfer.builder().id("txfr-001")
                .fromWarehouseId("wh-001").toWarehouseId("wh-002")
                .fromStockBefore(100).fromStockAfter(75)
                .toStockBefore(50).toStockAfter(75)
                .createdAt(Instant.now()).build();

        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-001", "wh-001"))
                .thenReturn(Optional.of(fromLevel));
        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-001", "wh-002"))
                .thenReturn(Optional.of(toLevel));
        when(inventoryLevelRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any())).thenReturn(movement);
        when(stockTransferRepository.save(any())).thenReturn(transfer);

        TransferResponse result = transferService.transfer(req, "user-001");

        assertThat(result.getTransferId()).isEqualTo("txfr-001");
        assertThat(fromLevel.getQuantityOnHand()).isEqualTo(75);
        assertThat(toLevel.getQuantityOnHand()).isEqualTo(75);
        verify(eventPublisher).publishStockTransferred(any());
    }

    @Test
    void transfer_insufficientStock_throwsException() {
        TransferRequest req = new TransferRequest();
        req.setProductId("prod-001");
        req.setFromWarehouseId("wh-001");
        req.setToWarehouseId("wh-002");
        req.setQuantity(200);

        InventoryLevel fromLevel = InventoryLevel.builder()
                .productId("prod-001").warehouseId("wh-001")
                .quantityOnHand(50).quantityReserved(0).build();

        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-001", "wh-001"))
                .thenReturn(Optional.of(fromLevel));

        assertThatThrownBy(() -> transferService.transfer(req, "user-001"))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void transfer_sameWarehouse_throwsIllegalArgument() {
        TransferRequest req = new TransferRequest();
        req.setProductId("prod-001");
        req.setFromWarehouseId("wh-001");
        req.setToWarehouseId("wh-001");
        req.setQuantity(10);

        assertThatThrownBy(() -> transferService.transfer(req, "user-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");
    }
}
