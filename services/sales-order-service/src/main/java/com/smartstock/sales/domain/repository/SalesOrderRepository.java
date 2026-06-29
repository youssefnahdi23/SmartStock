package com.smartstock.sales.domain.repository;

import com.smartstock.sales.domain.model.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, String> {

    boolean existsBySoNumber(String soNumber);

    @Query("""
            SELECT so FROM SalesOrder so WHERE
            (:status IS NULL OR so.status = :status)
            AND (:customerId IS NULL OR so.customerId = :customerId)
            AND (:fulfillmentStatus IS NULL OR so.fulfillmentStatus = :fulfillmentStatus)
            AND (:fromDate IS NULL OR so.orderDate >= :fromDate)
            AND (:toDate IS NULL OR so.orderDate <= :toDate)
            """)
    Page<SalesOrder> findWithFilters(
            @Param("status") String status,
            @Param("customerId") String customerId,
            @Param("fulfillmentStatus") String fulfillmentStatus,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    List<SalesOrder> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    @Query("SELECT so FROM SalesOrder so WHERE so.status IN ('CONFIRMED', 'PICKING') ORDER BY so.dueDate ASC")
    List<SalesOrder> findPendingDelivery();

    @Query("""
            SELECT COUNT(so) FROM SalesOrder so WHERE
            (:fromDate IS NULL OR so.orderDate >= :fromDate)
            AND (:toDate IS NULL OR so.orderDate <= :toDate)
            """)
    long countByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
