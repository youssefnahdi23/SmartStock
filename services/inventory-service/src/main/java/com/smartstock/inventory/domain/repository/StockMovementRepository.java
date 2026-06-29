package com.smartstock.inventory.domain.repository;

import com.smartstock.inventory.domain.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, String> {

    @Query("""
            SELECT sm FROM StockMovement sm
            WHERE (:productId IS NULL OR sm.productId = :productId)
            AND (:warehouseId IS NULL OR sm.warehouseId = :warehouseId)
            AND (:movementType IS NULL OR sm.movementType = :movementType)
            AND (:actorId IS NULL OR sm.actorId = :actorId)
            AND (:fromDate IS NULL OR sm.timestamp >= :fromDate)
            AND (:toDate IS NULL OR sm.timestamp <= :toDate)
            ORDER BY sm.timestamp DESC
            """)
    Page<StockMovement> findWithFilters(
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("movementType") String movementType,
            @Param("actorId") String actorId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
}
