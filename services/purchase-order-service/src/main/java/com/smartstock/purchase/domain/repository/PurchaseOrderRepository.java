package com.smartstock.purchase.domain.repository;

import com.smartstock.purchase.domain.model.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {

    boolean existsByPoNumber(String poNumber);

    @Query("""
            SELECT po FROM PurchaseOrder po
            WHERE (:status IS NULL OR po.status = :status)
              AND (:supplierId IS NULL OR po.supplierId = :supplierId)
              AND (:warehouseId IS NULL OR po.deliveryWarehouseId = :warehouseId)
              AND (:fromDate IS NULL OR po.orderDate >= :fromDate)
              AND (:toDate IS NULL OR po.orderDate <= :toDate)
            """)
    Page<PurchaseOrder> findWithFilters(
            @Param("status") String status,
            @Param("supplierId") String supplierId,
            @Param("warehouseId") String warehouseId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
