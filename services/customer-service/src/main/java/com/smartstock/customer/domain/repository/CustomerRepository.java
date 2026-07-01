package com.smartstock.customer.domain.repository;

import com.smartstock.customer.domain.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    boolean existsByCustomerCode(String customerCode);

    Optional<Customer> findByCustomerCode(String customerCode);

    @Query("""
            SELECT c FROM Customer c
            WHERE (:type IS NULL OR c.customerType = :type)
              AND (:segment IS NULL OR c.segment = :segment)
              AND (:status IS NULL OR
                   (:status = 'ACTIVE' AND c.isActive = true AND c.suspensionReason IS NULL) OR
                   (:status = 'INACTIVE' AND c.isActive = false AND c.suspensionReason IS NULL) OR
                   (:status = 'SUSPENDED' AND c.isActive = false AND c.suspensionReason IS NOT NULL))
              AND (:search IS NULL OR LOWER(c.customerName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<Customer> findWithFilters(
            @Param("type") String type,
            @Param("segment") String segment,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.segment = :segment AND c.isActive = true")
    Page<Customer> findBySegment(@Param("segment") String segment, Pageable pageable);
}
