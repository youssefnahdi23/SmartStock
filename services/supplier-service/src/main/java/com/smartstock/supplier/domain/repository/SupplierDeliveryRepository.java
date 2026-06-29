package com.smartstock.supplier.domain.repository;

import com.smartstock.supplier.domain.model.SupplierDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierDeliveryRepository extends JpaRepository<SupplierDelivery, String> {

    boolean existsByDeliveryNumber(String deliveryNumber);

    Optional<SupplierDelivery> findByDeliveryNumber(String deliveryNumber);

    @Query("""
            SELECT d FROM SupplierDelivery d
            WHERE d.supplierId = :supplierId
              AND (:status IS NULL OR d.deliveryStatus = :status)
              AND (:fromDate IS NULL OR d.orderDate >= :fromDate)
              AND (:toDate IS NULL OR d.orderDate <= :toDate)
            ORDER BY d.orderDate DESC
            """)
    Page<SupplierDelivery> findBySupplierWithFilters(
            @Param("supplierId") String supplierId,
            @Param("status") String status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @Query("""
            SELECT d FROM SupplierDelivery d
            WHERE d.supplierId = :supplierId
              AND d.orderDate BETWEEN :fromDate AND :toDate
              AND d.deliveryStatus = 'DELIVERED'
            """)
    List<SupplierDelivery> findDeliveredInPeriod(
            @Param("supplierId") String supplierId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT COUNT(d) FROM SupplierDelivery d
            WHERE d.supplierId = :supplierId
              AND d.deliveryStatus = 'DELIVERED'
              AND d.onTime = true
              AND d.orderDate BETWEEN :fromDate AND :toDate
            """)
    long countOnTimeDeliveries(@Param("supplierId") String supplierId,
                               @Param("fromDate") LocalDate fromDate,
                               @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT COUNT(d) FROM SupplierDelivery d
            WHERE d.supplierId = :supplierId
              AND d.deliveryStatus = 'DELIVERED'
              AND d.orderDate BETWEEN :fromDate AND :toDate
            """)
    long countDeliveries(@Param("supplierId") String supplierId,
                         @Param("fromDate") LocalDate fromDate,
                         @Param("toDate") LocalDate toDate);
}
