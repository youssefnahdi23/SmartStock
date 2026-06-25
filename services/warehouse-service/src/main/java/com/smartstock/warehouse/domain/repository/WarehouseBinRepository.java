package com.smartstock.warehouse.domain.repository;

import com.smartstock.warehouse.domain.model.WarehouseBin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseBinRepository extends JpaRepository<WarehouseBin, String> {

    @Query("SELECT b FROM WarehouseBin b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<WarehouseBin> findByIdAndNotDeleted(@Param("id") String id);

    boolean existsByCodeAndDeletedAtIsNull(String code);

    @Query("SELECT b FROM WarehouseBin b WHERE b.shelf.id = :shelfId AND b.deletedAt IS NULL")
    List<WarehouseBin> findByShelfId(@Param("shelfId") String shelfId);

    @Query("""
            SELECT b FROM WarehouseBin b
            JOIN b.shelf s
            JOIN s.zone z
            WHERE z.warehouse.id = :warehouseId
            AND b.deletedAt IS NULL
            AND b.active = true
            AND b.full = false
            AND (:zoneId IS NULL OR z.id = :zoneId)
            AND (b.capacityUnits - b.currentUnits) >= :quantity
            ORDER BY (b.capacityUnits - b.currentUnits) DESC
            """)
    List<WarehouseBin> findAvailableBins(
            @Param("warehouseId") String warehouseId,
            @Param("zoneId") String zoneId,
            @Param("quantity") int quantity);

    @Query("""
            SELECT COUNT(b) FROM WarehouseBin b
            JOIN b.shelf s
            JOIN s.zone z
            WHERE z.warehouse.id = :warehouseId
            AND b.deletedAt IS NULL
            """)
    long countByWarehouseId(@Param("warehouseId") String warehouseId);

    @Query("""
            SELECT COUNT(b) FROM WarehouseBin b
            JOIN b.shelf s
            JOIN s.zone z
            WHERE z.warehouse.id = :warehouseId
            AND b.deletedAt IS NULL
            AND b.currentUnits > 0
            """)
    long countOccupiedByWarehouseId(@Param("warehouseId") String warehouseId);
}
