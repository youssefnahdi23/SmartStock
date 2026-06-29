package com.smartstock.inventory.domain.repository;

import com.smartstock.inventory.domain.model.InventoryCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryCountRepository extends JpaRepository<InventoryCount, String> {
    List<InventoryCount> findByWarehouseIdAndStatus(String warehouseId, String status);
    boolean existsByWarehouseIdAndStatus(String warehouseId, String status);
}
