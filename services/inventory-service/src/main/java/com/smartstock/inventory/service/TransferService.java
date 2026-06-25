package com.smartstock.inventory.service;

import com.smartstock.inventory.api.dto.request.TransferRequest;
import com.smartstock.inventory.api.dto.response.TransferResponse;
import com.smartstock.inventory.domain.event.LowStockThresholdReachedEvent;
import com.smartstock.inventory.domain.event.StockTransferredEvent;
import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.model.StockMovement;
import com.smartstock.inventory.domain.model.StockTransfer;
import com.smartstock.inventory.domain.repository.InventoryLevelRepository;
import com.smartstock.inventory.domain.repository.StockMovementRepository;
import com.smartstock.inventory.domain.repository.StockTransferRepository;
import com.smartstock.inventory.exception.InsufficientStockException;
import com.smartstock.inventory.exception.InventoryLevelNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final InventoryLevelRepository inventoryLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockTransferRepository stockTransferRepository;
    private final InventoryEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_inventory:write') and hasAuthority('PERMISSION_stock:transfer')")
    public TransferResponse transfer(TransferRequest req, String actorId) {
        if (req.getFromWarehouseId().equals(req.getToWarehouseId())) {
            throw new IllegalArgumentException("Source and destination warehouses must be different");
        }

        InventoryLevel fromLevel = inventoryLevelRepository
                .findByProductIdAndWarehouseId(req.getProductId(), req.getFromWarehouseId())
                .orElseThrow(() -> new InventoryLevelNotFoundException(req.getProductId(), req.getFromWarehouseId()));

        if (req.getQuantity() > fromLevel.getQuantityAvailable()) {
            throw new InsufficientStockException(req.getProductId(), req.getFromWarehouseId(),
                    req.getQuantity(), fromLevel.getQuantityAvailable());
        }

        InventoryLevel toLevel = inventoryLevelRepository
                .findByProductIdAndWarehouseId(req.getProductId(), req.getToWarehouseId())
                .orElseGet(() -> InventoryLevel.builder()
                        .productId(req.getProductId())
                        .warehouseId(req.getToWarehouseId())
                        .unitCost(fromLevel.getUnitCost())
                        .build());

        int fromBefore = fromLevel.getQuantityOnHand();
        int toBefore = toLevel.getQuantityOnHand();

        fromLevel.dispatchStock(req.getQuantity());
        toLevel.receiveStock(req.getQuantity(), fromLevel.getUnitCost());

        inventoryLevelRepository.save(fromLevel);
        inventoryLevelRepository.save(toLevel);

        StockMovement movement = stockMovementRepository.save(StockMovement.builder()
                .movementType("TRANSFER")
                .productId(req.getProductId())
                .warehouseId(req.getFromWarehouseId())
                .quantity(req.getQuantity())
                .previousBalance(fromBefore)
                .newBalance(fromLevel.getQuantityOnHand())
                .movementReason(req.getReason())
                .notes(req.getNotes())
                .actorId(actorId)
                .build());

        StockTransfer transfer = stockTransferRepository.save(StockTransfer.builder()
                .stockMovementId(movement.getId())
                .fromWarehouseId(req.getFromWarehouseId())
                .toWarehouseId(req.getToWarehouseId())
                .transferStatus("CREATED")
                .transferReason(req.getReason())
                .fromStockBefore(fromBefore)
                .fromStockAfter(fromLevel.getQuantityOnHand())
                .toStockBefore(toBefore)
                .toStockAfter(toLevel.getQuantityOnHand())
                .shippedBy(actorId)
                .shippedAt(Instant.now())
                .notes(req.getNotes())
                .build());

        log.info("Transfer: product={} from={} to={} qty={}", req.getProductId(), req.getFromWarehouseId(), req.getToWarehouseId(), req.getQuantity());
        eventPublisher.publishStockTransferred(new StockTransferredEvent(transfer.getId(),
                req.getProductId(), req.getFromWarehouseId(), req.getToWarehouseId(),
                req.getQuantity(), fromBefore, fromLevel.getQuantityOnHand(),
                toBefore, toLevel.getQuantityOnHand(), req.getReason(), actorId));

        if (fromLevel.isLowStock()) {
            eventPublisher.publishLowStockThreshold(new LowStockThresholdReachedEvent(
                    req.getProductId(), req.getFromWarehouseId(), fromLevel.getQuantityOnHand(),
                    fromLevel.getReorderPoint(), fromLevel.getReorderQuantity(), fromLevel.getReorderPoint()));
        }

        return TransferResponse.builder()
                .transferId(transfer.getId())
                .productId(req.getProductId())
                .productName(req.getProductId())
                .fromWarehouseId(req.getFromWarehouseId())
                .fromWarehouseName(req.getFromWarehouseId())
                .toWarehouseId(req.getToWarehouseId())
                .toWarehouseName(req.getToWarehouseId())
                .quantity(req.getQuantity())
                .fromStockBefore(fromBefore)
                .fromStockAfter(fromLevel.getQuantityOnHand())
                .toStockBefore(toBefore)
                .toStockAfter(toLevel.getQuantityOnHand())
                .status("CREATED")
                .reason(req.getReason())
                .userId(actorId)
                .createdAt(transfer.getCreatedAt())
                .notes(req.getNotes())
                .build();
    }
}
