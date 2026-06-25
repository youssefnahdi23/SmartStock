package com.smartstock.inventory.service;

import com.smartstock.inventory.api.dto.request.StockInRequest;
import com.smartstock.inventory.api.dto.request.StockOutRequest;
import com.smartstock.inventory.api.dto.response.*;
import com.smartstock.inventory.domain.event.LowStockThresholdReachedEvent;
import com.smartstock.inventory.domain.event.StockInEvent;
import com.smartstock.inventory.domain.event.StockOutEvent;
import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.model.StockMovement;
import com.smartstock.inventory.domain.model.StockIn;
import com.smartstock.inventory.domain.model.StockOut;
import com.smartstock.inventory.domain.repository.*;
import com.smartstock.inventory.exception.InsufficientStockException;
import com.smartstock.inventory.exception.InventoryLevelNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryLevelRepository inventoryLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockInRepository stockInRepository;
    private final StockOutRepository stockOutRepository;
    private final InventoryEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_inventory:write') and hasAuthority('PERMISSION_stock:in')")
    public StockTransactionResponse receiveStock(StockInRequest req, String actorId) {
        InventoryLevel level = inventoryLevelRepository
                .findByProductIdAndWarehouseId(req.getProductId(), req.getWarehouseId())
                .orElseGet(() -> InventoryLevel.builder()
                        .productId(req.getProductId())
                        .warehouseId(req.getWarehouseId())
                        .unitCost(req.getUnitCost() != null ? req.getUnitCost() : BigDecimal.ZERO)
                        .build());

        int previousBalance = level.getQuantityOnHand();
        level.receiveStock(req.getQuantity(), req.getUnitCost());
        inventoryLevelRepository.save(level);

        StockMovement movement = stockMovementRepository.save(StockMovement.builder()
                .movementType("STOCK_IN")
                .productId(req.getProductId())
                .warehouseId(req.getWarehouseId())
                .quantity(req.getQuantity())
                .unitCost(req.getUnitCost())
                .movementTotal(req.getUnitCost() != null
                        ? req.getUnitCost().multiply(BigDecimal.valueOf(req.getQuantity())) : null)
                .previousBalance(previousBalance)
                .newBalance(level.getQuantityOnHand())
                .referenceType(req.getReferenceType())
                .referenceId(req.getReferenceId())
                .movementReason(req.getReferenceType())
                .notes(req.getNotes())
                .actorId(actorId)
                .build());

        stockInRepository.save(StockIn.builder()
                .stockMovementId(movement.getId())
                .supplierId(req.getSupplierId())
                .purchaseOrderId(req.getReferenceId())
                .quantityReceived(req.getQuantity())
                .quantityAccepted(req.getQuantity())
                .receivedAt(Instant.now())
                .receivedBy(actorId)
                .build());

        log.info("Stock IN: product={} warehouse={} qty={} actor={}", req.getProductId(), req.getWarehouseId(), req.getQuantity(), actorId);
        eventPublisher.publishStockIn(new StockInEvent(movement.getId(), req.getProductId(),
                req.getWarehouseId(), null, req.getQuantity(), req.getUnitCost(),
                req.getReferenceType(), req.getReferenceId(), req.getSupplierId(),
                previousBalance, level.getQuantityOnHand(), actorId));

        return toTransactionResponse(movement, "STOCK_IN", req.getSupplierId(), null);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_inventory:write') and hasAuthority('PERMISSION_stock:out')")
    public StockTransactionResponse dispatchStock(StockOutRequest req, String actorId) {
        InventoryLevel level = inventoryLevelRepository
                .findByProductIdAndWarehouseId(req.getProductId(), req.getWarehouseId())
                .orElseThrow(() -> new InventoryLevelNotFoundException(req.getProductId(), req.getWarehouseId()));

        if (req.getQuantity() > level.getQuantityAvailable()) {
            throw new InsufficientStockException(req.getProductId(), req.getWarehouseId(),
                    req.getQuantity(), level.getQuantityAvailable());
        }

        int previousBalance = level.getQuantityOnHand();
        level.dispatchStock(req.getQuantity());
        inventoryLevelRepository.save(level);

        StockMovement movement = stockMovementRepository.save(StockMovement.builder()
                .movementType("STOCK_OUT")
                .productId(req.getProductId())
                .warehouseId(req.getWarehouseId())
                .quantity(req.getQuantity())
                .previousBalance(previousBalance)
                .newBalance(level.getQuantityOnHand())
                .referenceType(req.getReferenceType())
                .referenceId(req.getReferenceId())
                .notes(req.getNotes())
                .actorId(actorId)
                .build());

        stockOutRepository.save(StockOut.builder()
                .stockMovementId(movement.getId())
                .orderId(req.getReferenceId())
                .customerId(req.getCustomerId())
                .shippedAt(Instant.now())
                .shippedBy(actorId)
                .build());

        log.info("Stock OUT: product={} warehouse={} qty={} actor={}", req.getProductId(), req.getWarehouseId(), req.getQuantity(), actorId);
        eventPublisher.publishStockOut(new StockOutEvent(movement.getId(), req.getProductId(),
                req.getWarehouseId(), req.getQuantity(), null, null,
                req.getReferenceType(), req.getReferenceType(), req.getReferenceId(),
                req.getCustomerId(), previousBalance, level.getQuantityOnHand(), actorId));

        if (level.isLowStock()) {
            eventPublisher.publishLowStockThreshold(new LowStockThresholdReachedEvent(
                    req.getProductId(), req.getWarehouseId(), level.getQuantityOnHand(),
                    level.getReorderPoint(), level.getReorderQuantity(), level.getReorderPoint()));
        }

        return toTransactionResponse(movement, "STOCK_OUT", null, req.getCustomerId());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_inventory:read')")
    public StockLevelResponse getStockLevel(String productId, String warehouseId) {
        InventoryLevel level = inventoryLevelRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new InventoryLevelNotFoundException(productId, warehouseId));
        return toStockLevelResponse(level);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_inventory:read')")
    public ProductStockResponse getProductStock(String productId) {
        List<InventoryLevel> levels = inventoryLevelRepository.findAllByProductId(productId);
        int total = levels.stream().mapToInt(InventoryLevel::getQuantityOnHand).sum();
        BigDecimal totalValue = levels.stream()
                .map(InventoryLevel::getInventoryValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ProductStockResponse.WarehouseStockEntry> warehouses = levels.stream()
                .map(l -> ProductStockResponse.WarehouseStockEntry.builder()
                        .warehouseId(l.getWarehouseId())
                        .warehouseName(l.getWarehouseId())
                        .stockLevel(l.getQuantityOnHand())
                        .reservedStock(l.getQuantityReserved())
                        .availableStock(l.getQuantityAvailable())
                        .percentage(total > 0 ? (double) l.getQuantityOnHand() / total * 100 : 0.0)
                        .build())
                .toList();

        return ProductStockResponse.builder()
                .productId(productId)
                .productName(productId)
                .totalStockLevel(total)
                .totalStockValue(totalValue)
                .warehouses(warehouses)
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_inventory:read')")
    public PagedResponse<WarehouseStockItemResponse> getWarehouseStock(
            String warehouseId, boolean lowStockOnly, String search, Pageable pageable) {

        Page<InventoryLevel> page = inventoryLevelRepository
                .findByWarehouseWithFilters(warehouseId, lowStockOnly, search, pageable);

        List<WarehouseStockItemResponse> items = page.getContent().stream()
                .map(l -> WarehouseStockItemResponse.builder()
                        .productId(l.getProductId())
                        .productName(l.getProductId())
                        .stockLevel(l.getQuantityOnHand())
                        .reservedStock(l.getQuantityReserved())
                        .availableStock(l.getQuantityAvailable())
                        .reorderPoint(l.getReorderPoint())
                        .stockValue(l.getInventoryValue())
                        .lowStock(l.isLowStock())
                        .lastMovementAt(l.getLastMovedAt())
                        .build())
                .toList();

        BigDecimal totalValue = page.getContent().stream()
                .map(InventoryLevel::getInventoryValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PagedResponse.<WarehouseStockItemResponse>builder()
                .data(items)
                .meta(PagedResponse.Meta.builder()
                        .timestamp(Instant.now())
                        .page(pageable.getPageNumber())
                        .size(pageable.getPageSize())
                        .total(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .totalStockValue(totalValue)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERMISSION_inventory:read')")
    public PagedResponse<TransactionResponse> getTransactions(
            String productId, String warehouseId, String movementType,
            String actorId, Instant fromDate, Instant toDate, Pageable pageable) {

        Page<StockMovement> page = stockMovementRepository.findWithFilters(
                productId, warehouseId, movementType, actorId, fromDate, toDate, pageable);

        List<TransactionResponse> items = page.getContent().stream()
                .map(m -> TransactionResponse.builder()
                        .transactionId(m.getId())
                        .productId(m.getProductId())
                        .productName(m.getProductId())
                        .warehouseId(m.getWarehouseId())
                        .warehouseName(m.getWarehouseId())
                        .quantity(m.getQuantity())
                        .previousBalance(m.getPreviousBalance())
                        .newBalance(m.getNewBalance())
                        .transactionType(m.getMovementType())
                        .referenceType(m.getReferenceType())
                        .referenceId(m.getReferenceId())
                        .userId(m.getActorId())
                        .username(m.getActorId())
                        .timestamp(m.getTimestamp())
                        .notes(m.getNotes())
                        .build())
                .toList();

        return PagedResponse.<TransactionResponse>builder()
                .data(items)
                .meta(PagedResponse.Meta.builder()
                        .timestamp(Instant.now())
                        .page(pageable.getPageNumber())
                        .size(pageable.getPageSize())
                        .total(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    private StockTransactionResponse toTransactionResponse(StockMovement m, String type,
                                                             String supplierId, String customerId) {
        return StockTransactionResponse.builder()
                .transactionId(m.getId())
                .productId(m.getProductId())
                .productName(m.getProductId())
                .warehouseId(m.getWarehouseId())
                .warehouseName(m.getWarehouseId())
                .quantity(m.getQuantity())
                .unitCost(m.getUnitCost())
                .transactionValue(m.getMovementTotal())
                .previousStockLevel(m.getPreviousBalance())
                .newStockLevel(m.getNewBalance())
                .transactionType(type)
                .referenceType(m.getReferenceType())
                .referenceId(m.getReferenceId())
                .supplierId(supplierId)
                .customerId(customerId)
                .userId(m.getActorId())
                .timestamp(m.getTimestamp())
                .notes(m.getNotes())
                .build();
    }

    private StockLevelResponse toStockLevelResponse(InventoryLevel l) {
        return StockLevelResponse.builder()
                .productId(l.getProductId())
                .productName(l.getProductId())
                .warehouseId(l.getWarehouseId())
                .warehouseName(l.getWarehouseId())
                .currentStockLevel(l.getQuantityOnHand())
                .reservedStock(l.getQuantityReserved())
                .availableStock(l.getQuantityAvailable())
                .reorderPoint(l.getReorderPoint())
                .reorderQuantity(l.getReorderQuantity())
                .maxStock(l.getMaxStock())
                .unitPrice(l.getUnitCost())
                .stockValue(l.getInventoryValue())
                .lowStock(l.isLowStock())
                .lastMovementAt(l.getLastMovedAt())
                .build();
    }
}
