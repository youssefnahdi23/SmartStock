package com.smartstock.inventory.domain.repository;

import com.smartstock.inventory.domain.model.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, String> {
    Optional<StockTransfer> findByStockMovementId(String stockMovementId);
}
