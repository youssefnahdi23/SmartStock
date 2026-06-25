package com.smartstock.inventory.domain.repository;

import com.smartstock.inventory.domain.model.StockIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockInRepository extends JpaRepository<StockIn, String> {
    Optional<StockIn> findByStockMovementId(String stockMovementId);
}
