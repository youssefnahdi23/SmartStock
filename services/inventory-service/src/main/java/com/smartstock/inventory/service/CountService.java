package com.smartstock.inventory.service;

import com.smartstock.inventory.api.dto.request.BeginCountRequest;
import com.smartstock.inventory.api.dto.request.CompleteCountRequest;
import com.smartstock.inventory.api.dto.request.RecordCountItemRequest;
import com.smartstock.inventory.api.dto.response.CompleteCountResponse;
import com.smartstock.inventory.api.dto.response.CountItemResponse;
import com.smartstock.inventory.api.dto.response.CountResponse;
import com.smartstock.inventory.domain.event.CountCompletedEvent;
import com.smartstock.inventory.domain.event.CountStartedEvent;
import com.smartstock.inventory.domain.model.*;
import com.smartstock.inventory.domain.repository.*;
import com.smartstock.inventory.exception.InventoryCountNotFoundException;
import com.smartstock.inventory.exception.InventoryLevelNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CountService {

    private final InventoryCountRepository countRepository;
    private final InventoryCountItemRepository countItemRepository;
    private final InventoryLevelRepository inventoryLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final InventoryEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_inventory:count')")
    public CountResponse beginCount(BeginCountRequest req, String actorId) {
        List<String> team = req.getCountTeam() != null ? req.getCountTeam() : List.of();
        String teamJson = "[" + String.join(",", team.stream().map(s -> "\"" + s + "\"").toList()) + "]";

        InventoryCount count = countRepository.save(InventoryCount.builder()
                .warehouseId(req.getWarehouseId())
                .countType(req.getCountType())
                .name(req.getName())
                .countDate(req.getCountDate())
                .countReason(req.getCountReason())
                .status("IN_PROGRESS")
                .expectedDuration(req.getExpectedDuration())
                .countTeam(teamJson)
                .createdBy(actorId)
                .startedAt(Instant.now())
                .build());

        log.info("Count started: id={} warehouse={} type={}", count.getId(), req.getWarehouseId(), req.getCountType());
        eventPublisher.publishCountStarted(new CountStartedEvent(count.getId(),
                req.getWarehouseId(), req.getCountType(), req.getName(), actorId));

        return toCountResponse(count, team);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_inventory:count')")
    public CountItemResponse recordItem(String countId, RecordCountItemRequest req, String actorId) {
        InventoryCount count = countRepository.findById(countId)
                .orElseThrow(() -> new InventoryCountNotFoundException(countId));

        int systemQty = inventoryLevelRepository
                .findByProductIdAndWarehouseId(req.getProductId(), count.getWarehouseId())
                .map(InventoryLevel::getQuantityOnHand)
                .orElse(0);

        InventoryCountItem item = countItemRepository.save(InventoryCountItem.builder()
                .count(count)
                .productId(req.getProductId())
                .systemQuantity(systemQty)
                .countedQuantity(req.getCountedQuantity())
                .location(req.getLocation())
                .condition(req.getCondition())
                .recordedBy(actorId)
                .notes(req.getNotes())
                .build());

        count.setTotalItemsCounted(count.getTotalItemsCounted() + 1);
        if (item.getVariance() != 0) {
            count.setTotalVariances(count.getTotalVariances() + 1);
        }
        countRepository.save(count);

        return CountItemResponse.builder()
                .countItemId(item.getId())
                .countId(countId)
                .productId(req.getProductId())
                .productName(req.getProductId())
                .systemQuantity(systemQty)
                .countedQuantity(req.getCountedQuantity())
                .variance(item.getVariance())
                .variancePercentage(item.getVariancePercentage())
                .location(req.getLocation())
                .condition(req.getCondition())
                .recordedBy(actorId)
                .timestamp(item.getCreatedAt())
                .notes(req.getNotes())
                .build();
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_inventory:count')")
    public CompleteCountResponse completeCount(String countId, CompleteCountRequest req, String actorId) {
        InventoryCount count = countRepository.findById(countId)
                .orElseThrow(() -> new InventoryCountNotFoundException(countId));

        List<InventoryCountItem> variances = countItemRepository.findVarianceItems(countId);
        int adjustmentsCreated = 0;

        if (req.isAutoAdjust()) {
            for (InventoryCountItem item : variances) {
                try {
                    InventoryLevel level = inventoryLevelRepository
                            .findByProductIdAndWarehouseId(item.getProductId(), count.getWarehouseId())
                            .orElseThrow(() -> new InventoryLevelNotFoundException(item.getProductId(), count.getWarehouseId()));

                    int before = level.getQuantityOnHand();
                    level.applyAdjustment(item.getVariance());
                    inventoryLevelRepository.save(level);

                    StockMovement movement = stockMovementRepository.save(StockMovement.builder()
                            .movementType("ADJUSTMENT")
                            .productId(item.getProductId())
                            .warehouseId(count.getWarehouseId())
                            .quantity(Math.abs(item.getVariance()))
                            .previousBalance(before)
                            .newBalance(level.getQuantityOnHand())
                            .movementReason("COUNT_VARIANCE")
                            .notes("Auto-adjusted from count " + countId)
                            .actorId(actorId)
                            .build());

                    stockAdjustmentRepository.save(StockAdjustment.builder()
                            .stockMovementId(movement.getId())
                            .adjustmentType("COUNT_VARIANCE")
                            .adjustmentReason("Physical count variance")
                            .adjustmentQuantity(item.getVariance())
                            .previousQuantity(before)
                            .newQuantity(level.getQuantityOnHand())
                            .adjustedAt(Instant.now())
                            .adjustedBy(actorId)
                            .approvalStatus("APPROVED")
                            .approvedBy(actorId)
                            .approvedAt(Instant.now())
                            .approverNotes(req.getApproverComments())
                            .build());

                    adjustmentsCreated++;
                } catch (Exception e) {
                    log.warn("Could not auto-adjust for product {} during count {}: {}", item.getProductId(), countId, e.getMessage());
                }
            }
        }

        int totalItems = count.getTotalItemsCounted();
        int totalVar = variances.size();
        double varianceRate = totalItems > 0 ? (double) totalVar / totalItems * 100 : 0.0;

        count.setStatus("COMPLETED");
        count.setTotalVariances(totalVar);
        count.setAdjustmentsCreated(adjustmentsCreated);
        count.setVarianceRate(varianceRate);
        count.setCompletedBy(actorId);
        count.setCompletedAt(Instant.now());
        count.setApproverComments(req.getApproverComments());
        countRepository.save(count);

        log.info("Count completed: id={} items={} variances={} adjustments={}", countId, totalItems, totalVar, adjustmentsCreated);
        eventPublisher.publishCountCompleted(new CountCompletedEvent(countId,
                count.getWarehouseId(), totalItems, totalVar, varianceRate, adjustmentsCreated, actorId));

        return CompleteCountResponse.builder()
                .countId(countId)
                .warehouseId(count.getWarehouseId())
                .status("COMPLETED")
                .totalItemsCounted(totalItems)
                .totalVariances(totalVar)
                .varianceRate(varianceRate)
                .adjustmentsCreated(adjustmentsCreated)
                .completedBy(actorId)
                .completedAt(count.getCompletedAt())
                .approverComments(req.getApproverComments())
                .build();
    }

    private CountResponse toCountResponse(InventoryCount count, List<String> team) {
        return CountResponse.builder()
                .countId(count.getId())
                .warehouseId(count.getWarehouseId())
                .warehouseName(count.getWarehouseId())
                .countType(count.getCountType())
                .name(count.getName())
                .countDate(count.getCountDate())
                .countReason(count.getCountReason())
                .status(count.getStatus())
                .expectedDuration(count.getExpectedDuration())
                .countTeam(team)
                .totalItemsCounted(count.getTotalItemsCounted())
                .totalVariances(count.getTotalVariances())
                .createdBy(count.getCreatedBy())
                .startedAt(count.getStartedAt())
                .build();
    }
}
