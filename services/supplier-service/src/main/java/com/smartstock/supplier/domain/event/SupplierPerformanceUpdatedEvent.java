package com.smartstock.supplier.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SupplierPerformanceUpdatedEvent extends DomainEvent {

    private String supplierCode;
    private LocalDate metricDate;
    private BigDecimal onTimeDeliveryRate;
    private BigDecimal qualityPassRate;
    private BigDecimal overallPerformanceScore;
    private Integer totalOrders;
    private Integer completedOnTime;
    private Integer completedLate;
    private BigDecimal priceCompetitiveness;
    private BigDecimal averageLeadTimeDays;

    public SupplierPerformanceUpdatedEvent(String supplierId, String supplierCode, LocalDate metricDate,
                                            BigDecimal onTimeDeliveryRate, BigDecimal qualityPassRate,
                                            BigDecimal overallPerformanceScore,
                                            Integer totalOrders, Integer completedOnTime, Integer completedLate,
                                            BigDecimal priceCompetitiveness, BigDecimal averageLeadTimeDays) {
        super(supplierId, "Supplier", "supplier-service");
        this.supplierCode = supplierCode;
        this.metricDate = metricDate;
        this.onTimeDeliveryRate = onTimeDeliveryRate;
        this.qualityPassRate = qualityPassRate;
        this.overallPerformanceScore = overallPerformanceScore;
        this.totalOrders = totalOrders;
        this.completedOnTime = completedOnTime;
        this.completedLate = completedLate;
        this.priceCompetitiveness = priceCompetitiveness;
        this.averageLeadTimeDays = averageLeadTimeDays;
    }
}
