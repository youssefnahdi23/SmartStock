package com.smartstock.supplier.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SupplierPerformanceResponse {

    private String supplierId;
    private String supplierCode;
    private String supplierName;
    private LocalDate fromDate;
    private LocalDate toDate;

    private Long totalOrders;
    private Long onTimeDeliveries;
    private Long lateDeliveries;
    private BigDecimal onTimeDeliveryRate;
    private BigDecimal qualityPassRate;
    private BigDecimal averageQualityRating;
    private Integer qualityIssuesCount;
    private BigDecimal averageLeadTimeDays;
    private BigDecimal totalValueReceived;
    private BigDecimal overallPerformanceScore;

    private String orderVolumeTrend;
    private String qualityTrend;
    private String deliveryPerformanceTrend;
}
