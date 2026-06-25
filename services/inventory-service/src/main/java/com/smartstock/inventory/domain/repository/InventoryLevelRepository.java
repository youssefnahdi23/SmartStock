package com.smartstock.inventory.domain.repository;

import com.smartstock.inventory.domain.model.InventoryLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, String> {

    Optional<InventoryLevel> findByProductIdAndWarehouseId(String productId, String warehouseId);

    boolean existsByProductIdAndWarehouseId(String productId, String warehouseId);

    List<InventoryLevel> findAllByProductId(String productId);

    @Query("""
            SELECT il FROM InventoryLevel il
            WHERE il.warehouseId = :warehouseId
            AND (:lowStockOnly = false OR il.quantityOnHand <= il.reorderPoint)
            AND (:search IS NULL OR LOWER(il.productId) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<InventoryLevel> findByWarehouseWithFilters(
            @Param("warehouseId") String warehouseId,
            @Param("lowStockOnly") boolean lowStockOnly,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT il FROM InventoryLevel il WHERE il.warehouseId = :warehouseId AND il.quantityOnHand <= il.reorderPoint AND il.reorderPoint > 0")
    List<InventoryLevel> findLowStockByWarehouse(@Param("warehouseId") String warehouseId);
}
