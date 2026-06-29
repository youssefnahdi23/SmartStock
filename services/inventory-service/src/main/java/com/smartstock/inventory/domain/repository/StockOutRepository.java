package com.smartstock.inventory.domain.repository;

import com.smartstock.inventory.domain.model.StockOut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockOutRepository extends JpaRepository<StockOut, String> {
    Optional<StockOut> findByStockMovementId(String stockMovementId);
}
