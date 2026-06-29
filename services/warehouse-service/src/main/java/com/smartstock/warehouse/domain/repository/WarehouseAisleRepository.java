package com.smartstock.warehouse.domain.repository;

import com.smartstock.warehouse.domain.model.WarehouseAisle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseAisleRepository extends JpaRepository<WarehouseAisle, String> {

    @Query("SELECT a FROM WarehouseAisle a WHERE a.zone.id = :zoneId")
    List<WarehouseAisle> findByZoneId(@Param("zoneId") String zoneId);
}
