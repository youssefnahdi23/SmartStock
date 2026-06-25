package com.smartstock.warehouse.domain.repository;

import com.smartstock.warehouse.domain.model.WarehouseEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseEquipmentRepository extends JpaRepository<WarehouseEquipment, String> {

    @Query("SELECT e FROM WarehouseEquipment e WHERE e.warehouse.id = :warehouseId")
    List<WarehouseEquipment> findByWarehouseId(@Param("warehouseId") String warehouseId);

    @Query("SELECT e FROM WarehouseEquipment e WHERE e.warehouse.id = :warehouseId AND e.maintenanceStatus = :status")
    List<WarehouseEquipment> findByWarehouseIdAndMaintenanceStatus(
            @Param("warehouseId") String warehouseId,
            @Param("status") String status);
}
