package com.smartstock.inventory.unit;

import com.smartstock.inventory.api.dto.request.StockInRequest;
import com.smartstock.inventory.api.dto.request.StockOutRequest;
import com.smartstock.inventory.api.dto.response.StockTransactionResponse;
import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.model.StockMovement;
import com.smartstock.inventory.domain.model.StockIn;
import com.smartstock.inventory.domain.model.StockOut;
import com.smartstock.inventory.domain.repository.*;
import com.smartstock.inventory.exception.InsufficientStockException;
import com.smartstock.inventory.exception.InventoryLevelNotFoundException;
import com.smartstock.inventory.service.InventoryEventPublisher;
import com.smartstock.inventory.service.InventoryService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceUnitTest {

    @Mock InventoryLevelRepository inventoryLevelRepository;
    @Mock StockMovementRepository stockMovementRepository;
    @Mock StockInRepository stockInRepository;
    @Mock StockOutRepository stockOutRepository;
    @Mock InventoryEventPublisher eventPublisher;

    @InjectMocks InventoryService inventoryService;

    private StockMovement savedMovement;

    @BeforeEach
    void setUp() {
        savedMovement = StockMovement.builder()
                .id("mov-001")
                .movementType("STOCK_IN")
                .productId("prod-001")
                .warehouseId("wh-001")
                .quantity(100)
                .unitCost(BigDecimal.valueOf(45.00))
                .movementTotal(BigDecimal.valueOf(4500.00))
                .previousBalance(0)
                .newBalance(100)
                .actorId("user-001")
                .timestamp(Instant.now())
                .build();
    }

    @Test
    void stockIn_newProduct_createsInventoryLevelAndMovement() {
        StockInRequest req = new StockInRequest();
        req.setProductId("prod-001");
        req.setWarehouseId("wh-001");
        req.setQuantity(100);
        req.setUnitCost(BigDecimal.valueOf(45.00));
        req.setReferenceType("PURCHASE_ORDER");
        req.setReferenceId("PO-001");

        when(inventoryLevelRepository.findByProductIdAndWarehouseId(any(), any()))
                .thenReturn(Optional.empty());
        when(inventoryLevelRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any())).thenReturn(savedMovement);
        when(stockInRepository.save(any())).thenAnswer(i -> {
            StockIn si = i.getArgument(0);
            return si;
        });

        StockTransactionResponse result = inventoryService.receiveStock(req, "user-001");

        assertThat(result.getTransactionId()).isEqualTo("mov-001");
        assertThat(result.getTransactionType()).isEqualTo("STOCK_IN");
        assertThat(result.getProductId()).isEqualTo("prod-001");
        verify(inventoryLevelRepository).save(any());
        verify(stockMovementRepository).save(any());
        verify(stockInRepository).save(any());
        verify(eventPublisher).publishStockIn(any());
    }

    @Test
    void stockIn_existingProduct_updatesQuantity() {
        StockInRequest req = new StockInRequest();
        req.setProductId("prod-001");
        req.setWarehouseId("wh-001");
        req.setQuantity(50);
        req.setUnitCost(BigDecimal.valueOf(45.00));

        InventoryLevel existing = InventoryLevel.builder()
                .id("lvl-001")
                .productId("prod-001")
                .warehouseId("wh-001")
                .quantityOnHand(200)
                .unitCost(BigDecimal.valueOf(40.00))
                .build();

        StockMovement mv = StockMovement.builder()
                .id("mov-002").movementType("STOCK_IN").productId("prod-001").warehouseId("wh-001")
                .quantity(50).previousBalance(200).newBalance(250)
                .actorId("user-001").build();

        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-001", "wh-001"))
                .thenReturn(Optional.of(existing));
        when(inventoryLevelRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any())).thenReturn(mv);
        when(stockInRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StockTransactionResponse result = inventoryService.receiveStock(req, "user-001");

        assertThat(result.getQuantity()).isEqualTo(50);
        assertThat(existing.getQuantityOnHand()).isEqualTo(250);
    }

    @Test
    void stockOut_insufficientStock_throwsException() {
        StockOutRequest req = new StockOutRequest();
        req.setProductId("prod-001");
        req.setWarehouseId("wh-001");
        req.setQuantity(500);

        InventoryLevel level = InventoryLevel.builder()
                .productId("prod-001")
                .warehouseId("wh-001")
                .quantityOnHand(100)
                .quantityReserved(0)
                .build();

        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-001", "wh-001"))
                .thenReturn(Optional.of(level));

        assertThatThrownBy(() -> inventoryService.dispatchStock(req, "user-001"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void stockOut_sufficientStock_reducesQuantityAndPublishesEvent() {
        StockOutRequest req = new StockOutRequest();
        req.setProductId("prod-001");
        req.setWarehouseId("wh-001");
        req.setQuantity(60);  // 100 - 60 = 40, below reorderPoint of 50 → triggers low-stock event
        req.setReferenceType("SALES_ORDER");
        req.setReferenceId("SO-001");

        InventoryLevel level = InventoryLevel.builder()
                .id("lvl-001")
                .productId("prod-001")
                .warehouseId("wh-001")
                .quantityOnHand(100)
                .quantityReserved(0)
                .reorderPoint(50)
                .reorderQuantity(100)
                .unitCost(BigDecimal.valueOf(45.00))
                .build();

        StockMovement mv = StockMovement.builder()
                .id("mov-003").movementType("STOCK_OUT").productId("prod-001").warehouseId("wh-001")
                .quantity(60).previousBalance(100).newBalance(40)
                .actorId("user-001").build();

        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-001", "wh-001"))
                .thenReturn(Optional.of(level));
        when(inventoryLevelRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any())).thenReturn(mv);
        when(stockOutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StockTransactionResponse result = inventoryService.dispatchStock(req, "user-001");

        assertThat(result.getTransactionType()).isEqualTo("STOCK_OUT");
        assertThat(level.getQuantityOnHand()).isEqualTo(40);
        verify(eventPublisher).publishStockOut(any());
        verify(eventPublisher).publishLowStockThreshold(any());
    }

    @Test
    void getStockLevel_notFound_throwsException() {
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getStockLevel("prod-xxx", "wh-xxx"))
                .isInstanceOf(InventoryLevelNotFoundException.class);
    }
}
