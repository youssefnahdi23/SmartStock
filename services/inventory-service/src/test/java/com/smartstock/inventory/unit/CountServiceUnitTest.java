package com.smartstock.inventory.unit;

import com.smartstock.inventory.api.dto.request.BeginCountRequest;
import com.smartstock.inventory.api.dto.request.CompleteCountRequest;
import com.smartstock.inventory.api.dto.request.RecordCountItemRequest;
import com.smartstock.inventory.api.dto.response.CompleteCountResponse;
import com.smartstock.inventory.api.dto.response.CountItemResponse;
import com.smartstock.inventory.api.dto.response.CountResponse;
import com.smartstock.inventory.domain.model.InventoryCount;
import com.smartstock.inventory.domain.model.InventoryCountItem;
import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.repository.InventoryCountItemRepository;
import com.smartstock.inventory.domain.repository.InventoryCountRepository;
import com.smartstock.inventory.domain.repository.InventoryLevelRepository;
import com.smartstock.inventory.domain.repository.StockAdjustmentRepository;
import com.smartstock.inventory.domain.repository.StockMovementRepository;
import com.smartstock.inventory.exception.InventoryCountNotFoundException;
import com.smartstock.inventory.service.CountService;
import com.smartstock.inventory.service.InventoryEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountServiceUnitTest {

    @Mock InventoryCountRepository countRepository;
    @Mock InventoryCountItemRepository countItemRepository;
    @Mock InventoryLevelRepository inventoryLevelRepository;
    @Mock StockMovementRepository stockMovementRepository;
    @Mock StockAdjustmentRepository stockAdjustmentRepository;
    @Mock InventoryEventPublisher eventPublisher;

    @InjectMocks CountService countService;

    private InventoryCount savedCount;

    @BeforeEach
    void setUp() {
        savedCount = InventoryCount.builder()
                .id("count-001")
                .warehouseId("wh-001")
                .countType("FULL")
                .name("Q2 Full Count")
                .countDate(LocalDate.of(2026, 6, 25))
                .countReason("Quarterly audit")
                .status("IN_PROGRESS")
                .expectedDuration("4 hours")
                .countTeam("[\"user-001\",\"user-002\"]")
                .totalItemsCounted(0)
                .totalVariances(0)
                .adjustmentsCreated(0)
                .createdBy("user-001")
                .startedAt(Instant.now())
                .build();
    }

    @Test
    void beginCount_happyPath_savesCountAndPublishesEvent() {
        BeginCountRequest req = new BeginCountRequest();
        req.setWarehouseId("wh-001");
        req.setCountType("FULL");
        req.setName("Q2 Full Count");
        req.setCountDate(LocalDate.of(2026, 6, 25));
        req.setCountReason("Quarterly audit");
        req.setExpectedDuration("4 hours");
        req.setCountTeam(List.of("user-001", "user-002"));

        when(countRepository.save(any())).thenReturn(savedCount);

        CountResponse result = countService.beginCount(req, "user-001");

        assertThat(result.getCountId()).isEqualTo("count-001");
        assertThat(result.getWarehouseId()).isEqualTo("wh-001");
        assertThat(result.getCountType()).isEqualTo("FULL");
        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(result.getCreatedBy()).isEqualTo("user-001");

        verify(countRepository).save(any());
        verify(eventPublisher).publishCountStarted(any());
    }

    @Test
    void recordItem_happyPath_savesCountItemAndUpdatesCount() {
        RecordCountItemRequest req = new RecordCountItemRequest();
        req.setProductId("prod-001");
        req.setCountedQuantity(95);
        req.setLocation("A1-B2");
        req.setCondition("GOOD");

        InventoryLevel level = InventoryLevel.builder()
                .productId("prod-001").warehouseId("wh-001")
                .quantityOnHand(100).quantityReserved(0)
                .unitCost(BigDecimal.valueOf(45.00)).build();

        InventoryCountItem savedItem = InventoryCountItem.builder()
                .id("item-001")
                .count(savedCount)
                .productId("prod-001")
                .systemQuantity(100)
                .countedQuantity(95)
                .location("A1-B2")
                .condition("GOOD")
                .recordedBy("user-001")
                .createdAt(Instant.now())
                .build();

        when(countRepository.findById("count-001")).thenReturn(Optional.of(savedCount));
        when(inventoryLevelRepository.findByProductIdAndWarehouseId("prod-001", "wh-001"))
                .thenReturn(Optional.of(level));
        when(countItemRepository.save(any())).thenReturn(savedItem);
        when(countRepository.save(any())).thenReturn(savedCount);

        CountItemResponse result = countService.recordItem("count-001", req, "user-001");

        assertThat(result.getCountItemId()).isEqualTo("item-001");
        assertThat(result.getProductId()).isEqualTo("prod-001");
        assertThat(result.getSystemQuantity()).isEqualTo(100);
        assertThat(result.getCountedQuantity()).isEqualTo(95);
        assertThat(result.getVariance()).isEqualTo(-5);

        verify(countItemRepository).save(any());
        verify(countRepository).save(any());
    }

    @Test
    void completeCount_happyPath_marksCountCompleted() {
        CompleteCountRequest req = new CompleteCountRequest();
        req.setAutoAdjust(false);

        InventoryCount countInProgress = InventoryCount.builder()
                .id("count-001")
                .warehouseId("wh-001")
                .countType("FULL")
                .name("Q2 Full Count")
                .countDate(LocalDate.of(2026, 6, 25))
                .status("IN_PROGRESS")
                .totalItemsCounted(5)
                .totalVariances(0)
                .adjustmentsCreated(0)
                .createdBy("user-001")
                .startedAt(Instant.now())
                .build();

        when(countRepository.findById("count-001")).thenReturn(Optional.of(countInProgress));
        when(countItemRepository.findVarianceItems("count-001")).thenReturn(List.of());
        when(countRepository.save(any())).thenReturn(countInProgress);

        CompleteCountResponse result = countService.completeCount("count-001", req, "user-001");

        assertThat(result.getCountId()).isEqualTo("count-001");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTotalItemsCounted()).isEqualTo(5);
        assertThat(result.getTotalVariances()).isEqualTo(0);
        assertThat(result.getAdjustmentsCreated()).isEqualTo(0);
        assertThat(result.getCompletedBy()).isEqualTo("user-001");

        verify(countRepository).save(any());
        verify(eventPublisher).publishCountCompleted(any());
    }

    @Test
    void completeCount_countNotFound_throwsException() {
        when(countRepository.findById("count-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> countService.completeCount("count-999", new CompleteCountRequest(), "user-001"))
                .isInstanceOf(InventoryCountNotFoundException.class);

        verify(countRepository, never()).save(any());
        verify(eventPublisher, never()).publishCountCompleted(any());
    }
}
