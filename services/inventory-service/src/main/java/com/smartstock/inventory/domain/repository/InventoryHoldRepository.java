package com.smartstock.inventory.domain.repository;

import com.smartstock.inventory.domain.model.InventoryHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryHoldRepository extends JpaRepository<InventoryHold, String> {

    Optional<InventoryHold> findByOrderIdAndStatus(String orderId, String status);

    @Query("SELECT ih FROM InventoryHold ih WHERE ih.productId = :productId AND ih.warehouseId = :warehouseId AND ih.status = 'ACTIVE'")
    List<InventoryHold> findActiveHolds(@Param("productId") String productId,
                                        @Param("warehouseId") String warehouseId);
}
