package com.smartstock.inventory.domain.repository;

import com.smartstock.inventory.domain.model.InventoryCountItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryCountItemRepository extends JpaRepository<InventoryCountItem, String> {

    List<InventoryCountItem> findByCountId(String countId);

    @Query("SELECT i FROM InventoryCountItem i WHERE i.count.id = :countId AND i.countedQuantity <> i.systemQuantity")
    List<InventoryCountItem> findVarianceItems(@Param("countId") String countId);
}
