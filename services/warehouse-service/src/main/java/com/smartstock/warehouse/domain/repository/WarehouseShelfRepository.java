package com.smartstock.warehouse.domain.repository;

import com.smartstock.warehouse.domain.model.WarehouseShelf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseShelfRepository extends JpaRepository<WarehouseShelf, String> {

    @Query("SELECT s FROM WarehouseShelf s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<WarehouseShelf> findByIdAndNotDeleted(@Param("id") String id);

    boolean existsByZoneIdAndCodeAndDeletedAtIsNull(String zoneId, String code);

    @Query("SELECT s FROM WarehouseShelf s WHERE s.zone.id = :zoneId AND s.deletedAt IS NULL")
    List<WarehouseShelf> findByZoneId(@Param("zoneId") String zoneId);
}
