package com.smartstock.supplier.domain.repository;

import com.smartstock.supplier.domain.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {

    boolean existsBySupplierCode(String supplierCode);

    Optional<Supplier> findBySupplierCode(String supplierCode);

    @Query("""
            SELECT s FROM Supplier s
            WHERE (:type IS NULL OR s.supplierType = :type)
              AND (:status IS NULL OR
                   (:status = 'ACTIVE' AND s.isActive = true) OR
                   (:status = 'INACTIVE' AND s.isActive = false AND s.suspensionReason IS NULL) OR
                   (:status = 'SUSPENDED' AND s.isActive = false AND s.suspensionReason IS NOT NULL))
              AND (:search IS NULL OR LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:minRating IS NULL OR s.rating >= :minRating)
            """)
    Page<Supplier> findWithFilters(
            @Param("type") String type,
            @Param("status") String status,
            @Param("search") String search,
            @Param("minRating") Double minRating,
            Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE s.isActive = true ORDER BY s.rating DESC NULLS LAST")
    Page<Supplier> findTopRated(Pageable pageable);
}
