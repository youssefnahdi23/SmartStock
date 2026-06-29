package com.smartstock.supplier.domain.repository;

import com.smartstock.supplier.domain.model.SupplierContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SupplierContractRepository extends JpaRepository<SupplierContract, String> {

    boolean existsByContractNumber(String contractNumber);

    Page<SupplierContract> findBySupplierId(String supplierId, Pageable pageable);

    List<SupplierContract> findBySupplierIdAndContractStatus(String supplierId, String status);

    @Query("SELECT c FROM SupplierContract c WHERE c.endDate BETWEEN :now AND :threshold AND c.contractStatus = 'ACTIVE'")
    List<SupplierContract> findExpiringContracts(@Param("now") LocalDate now, @Param("threshold") LocalDate threshold);
}
