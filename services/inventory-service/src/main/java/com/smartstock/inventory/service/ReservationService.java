package com.smartstock.inventory.service;

import com.smartstock.inventory.api.dto.request.ReservationRequest;
import com.smartstock.inventory.api.dto.response.ReservationResponse;
import com.smartstock.inventory.domain.event.StockReservedEvent;
import com.smartstock.inventory.domain.model.InventoryHold;
import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.repository.InventoryHoldRepository;
import com.smartstock.inventory.domain.repository.InventoryLevelRepository;
import com.smartstock.inventory.exception.InsufficientStockException;
import com.smartstock.inventory.exception.InventoryLevelNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final InventoryLevelRepository inventoryLevelRepository;
    private final InventoryHoldRepository inventoryHoldRepository;
    private final InventoryEventPublisher eventPublisher;
    private final ConcurrencyRetry concurrencyRetry;
    private final ObjectProvider<ReservationService> self;

    @PreAuthorize("hasAuthority('PERMISSION_stock:reserve')")
    public ReservationResponse reserve(ReservationRequest req, String actorId) {
        return concurrencyRetry.execute(() -> self.getObject().reserveTransactional(req, actorId));
    }

    /** Called by Kafka consumers (no security context); retries on optimistic-lock conflict. */
    public ReservationResponse reserveInternal(ReservationRequest req, String actorId) {
        return concurrencyRetry.execute(() -> self.getObject().reserveTransactional(req, actorId));
    }

    @Transactional
    public ReservationResponse reserveTransactional(ReservationRequest req, String actorId) {
        InventoryLevel level = inventoryLevelRepository
                .findByProductIdAndWarehouseId(req.getProductId(), req.getWarehouseId())
                .orElseThrow(() -> new InventoryLevelNotFoundException(req.getProductId(), req.getWarehouseId()));

        if (req.getQuantity() > level.getQuantityAvailable()) {
            throw new InsufficientStockException(req.getProductId(), req.getWarehouseId(),
                    req.getQuantity(), level.getQuantityAvailable());
        }

        level.reserveStock(req.getQuantity());
        inventoryLevelRepository.save(level);

        Instant expiryInstant = req.getExpiryDate() != null
                ? req.getExpiryDate().atStartOfDay().toInstant(ZoneOffset.UTC) : null;

        InventoryHold hold = inventoryHoldRepository.save(InventoryHold.builder()
                .productId(req.getProductId())
                .warehouseId(req.getWarehouseId())
                .quantityHeld(req.getQuantity())
                .holdReason(req.getReservationReason() != null ? req.getReservationReason() : "RESERVATION")
                .orderId(req.getOrderId())
                .heldAt(Instant.now())
                .heldBy(actorId)
                .releaseDate(expiryInstant)
                .status("ACTIVE")
                .build());

        log.info("Reservation created: id={} product={} warehouse={} qty={}", hold.getId(), req.getProductId(), req.getWarehouseId(), req.getQuantity());
        eventPublisher.publishStockReserved(new StockReservedEvent(hold.getId(),
                req.getProductId(), req.getWarehouseId(), req.getQuantity(),
                req.getOrderId(), req.getReservationReason(), expiryInstant,
                actorId, level.getQuantityAvailable()));

        return ReservationResponse.builder()
                .reservationId(hold.getId())
                .productId(req.getProductId())
                .warehouseId(req.getWarehouseId())
                .quantity(req.getQuantity())
                .orderId(req.getOrderId())
                .reservationReason(req.getReservationReason())
                .status("ACTIVE")
                .createdAt(hold.getCreatedAt())
                .expiryDate(req.getExpiryDate())
                .availableStockAfterReservation(level.getQuantityAvailable())
                .build();
    }
}
