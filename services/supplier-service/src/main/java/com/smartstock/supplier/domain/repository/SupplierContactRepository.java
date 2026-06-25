package com.smartstock.supplier.domain.repository;

import com.smartstock.supplier.domain.model.SupplierContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierContactRepository extends JpaRepository<SupplierContact, String> {

    List<SupplierContact> findBySupplierId(String supplierId);

    List<SupplierContact> findBySupplierIdAndIsActiveTrue(String supplierId);

    Optional<SupplierContact> findBySupplierIdAndIsPrimaryTrue(String supplierId);
}
