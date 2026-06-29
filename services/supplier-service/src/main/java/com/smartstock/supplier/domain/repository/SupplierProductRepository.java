package com.smartstock.supplier.domain.repository;

import com.smartstock.supplier.domain.model.SupplierProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierProductRepository extends JpaRepository<SupplierProduct, String> {

    Page<SupplierProduct> findBySupplierIdAndIsActiveTrue(String supplierId, Pageable pageable);

    Optional<SupplierProduct> findBySupplierIdAndProductId(String supplierId, String productId);

    List<SupplierProduct> findByProductIdAndIsActiveTrue(String productId);

    long countBySupplierIdAndIsActiveTrue(String supplierId);
}
