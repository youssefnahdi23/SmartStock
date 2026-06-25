package com.smartstock.supplier.domain.repository;

import com.smartstock.supplier.domain.model.SupplierMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierMetricsRepository extends JpaRepository<SupplierMetrics, String> {

    Optional<SupplierMetrics> findBySupplierIdAndMetricDate(String supplierId, LocalDate metricDate);

    Page<SupplierMetrics> findBySupplierIdOrderByMetricDateDesc(String supplierId, Pageable pageable);

    List<SupplierMetrics> findBySupplierIdAndMetricDateBetweenOrderByMetricDateDesc(
            String supplierId, LocalDate fromDate, LocalDate toDate);
}
