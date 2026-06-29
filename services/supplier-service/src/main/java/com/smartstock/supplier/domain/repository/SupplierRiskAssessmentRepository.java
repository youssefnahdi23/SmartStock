package com.smartstock.supplier.domain.repository;

import com.smartstock.supplier.domain.model.SupplierRiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRiskAssessmentRepository extends JpaRepository<SupplierRiskAssessment, String> {

    List<SupplierRiskAssessment> findBySupplierIdOrderByAssessmentDateDesc(String supplierId);
}
