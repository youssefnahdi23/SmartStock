package com.smartstock.inventory.service;

import com.smartstock.inventory.api.dto.request.AdjustmentRequest;
import com.smartstock.inventory.api.dto.response.AdjustmentResponse;
import com.smartstock.inventory.domain.event.StockAdjustedEvent;
import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.model.StockAdjustment;
import com.smartstock.inventory.domain.model.StockMovement;
import com.smartstock.inventory.domain.repository.InventoryLevelRepository;
import com.smartstock.inventory.domain.repository.StockAdjustmentRepository;
import com.smartstock.inventory.domain.repository.StockMovementRepository;
import com.smartstock.inventory.exception.InventoryLevelNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdjustmentService {

    private final InventoryLevelRepository inventoryLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final InventoryEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_inventory:write') and hasAuthority('PERMISSION_stock:adjust')")
    public AdjustmentResponse adjust(AdjustmentRequest req, String actorId) {
        InventoryLevel level = inventoryLevelRepository
                .findByProductIdAndWarehouseId(req.getProductId(), req.getWarehouseId())
                .orElseThrow(() -> new InventoryLevelNotFoundException(req.getProductId(), req.getWarehouseId()));

        int previousQty = level.getQuantityOnHand();
        level.applyAdjustment(req.getAdjustmentQuantity());
        inventoryLevelRepository.save(level);

        String adjType = req.getAdjustmentType() != null ? req.getAdjustmentType() : "CORRECTION";

        StockMovement movement = stockMovementRepository.save(StockMovement.builder()
                .movementType("ADJUSTMENT")
                .productId(req.getProductId())
                .warehouseId(req.getWarehouseId())
                .quantity(Math.abs(req.getAdjustmentQuantity()))
                .previousBalance(previousQty)
                .newBalance(level.getQuantityOnHand())
                .movementReason(req.getReason())
                .notes(req.getNotes())
                .actorId(actorId)
                .build());

        StockAdjustment adjustment = stockAdjustmentRepository.save(StockAdjustment.builder()
                .stockMovementId(movement.getId())
                .adjustmentType(adjType)
                .adjustmentReason(req.getReason())
                .adjustmentQuantity(req.getAdjustmentQuantity())
                .previousQuantity(previousQty)
                .newQuantity(level.getQuantityOnHand())
                .adjustedAt(Instant.now())
                .adjustedBy(actorId)
                .approvalStatus("APPROVED")
                .approvedBy(actorId)
                .approvedAt(Instant.now())
                .approverNotes(req.getApproverComments())
                .build());

        log.info("Adjustment: product={} warehouse={} delta={} actor={}", req.getProductId(), req.getWarehouseId(), req.getAdjustmentQuantity(), actorId);
        eventPublisher.publishStockAdjusted(new StockAdjustedEvent(adjustment.getId(),
                req.getProductId(), req.getWarehouseId(), previousQty, level.getQuantityOnHand(),
                req.getReason(), actorId, req.getNotes()));

        return AdjustmentResponse.builder()
                .adjustmentId(adjustment.getId())
                .productId(req.getProductId())
                .productName(req.getProductId())
                .warehouseId(req.getWarehouseId())
                .warehouseName(req.getWarehouseId())
                .adjustmentQuantity(req.getAdjustmentQuantity())
                .reason(req.getReason())
                .adjustmentType(adjType)
                .previousStockLevel(previousQty)
                .newStockLevel(level.getQuantityOnHand())
                .status("APPROVED")
                .createdBy(actorId)
                .approvedBy(actorId)
                .createdAt(adjustment.getCreatedAt())
                .approvedAt(adjustment.getApprovedAt())
                .notes(req.getNotes())
                .approverComments(req.getApproverComments())
                .build();
    }
}
