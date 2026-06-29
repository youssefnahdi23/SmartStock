package com.smartstock.inventory.domain.repository;

import com.smartstock.inventory.domain.model.DamagedInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DamagedInventoryRepository extends JpaRepository<DamagedInventory, String> {
    List<DamagedInventory> findByProductIdAndWarehouseId(String productId, String warehouseId);
    List<DamagedInventory> findByWarehouseIdAndResolvedAtIsNull(String warehouseId);
}
