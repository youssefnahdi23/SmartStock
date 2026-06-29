package com.smartstock.warehouse.domain.repository;

import com.smartstock.warehouse.domain.model.WarehouseZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseZoneRepository extends JpaRepository<WarehouseZone, String> {

    @Query("SELECT z FROM WarehouseZone z WHERE z.id = :id AND z.deletedAt IS NULL")
    Optional<WarehouseZone> findByIdAndNotDeleted(@Param("id") String id);

    boolean existsByWarehouseIdAndCodeAndDeletedAtIsNull(String warehouseId, String code);

    @Query("SELECT z FROM WarehouseZone z WHERE z.warehouse.id = :warehouseId AND z.deletedAt IS NULL")
    Page<WarehouseZone> findByWarehouseId(@Param("warehouseId") String warehouseId, Pageable pageable);

    @Query("SELECT z FROM WarehouseZone z WHERE z.warehouse.id = :warehouseId AND z.deletedAt IS NULL")
    List<WarehouseZone> findAllByWarehouseId(@Param("warehouseId") String warehouseId);
}
