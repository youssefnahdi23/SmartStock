package com.smartstock.inventory.domain.repository;

import com.smartstock.inventory.domain.model.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, String> {
    List<StockAdjustment> findByStockMovementId(String stockMovementId);
}
