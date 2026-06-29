package com.smartstock.supplier.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "supplier_metrics",
        uniqueConstraints = @UniqueConstraint(name = "uq_supplier_metric", columnNames = {"supplier_id", "metric_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "total_orders")
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "total_units_received")
    @Builder.Default
    private Integer totalUnitsReceived = 0;

    @Column(name = "on_time_deliveries")
    @Builder.Default
    private Integer onTimeDeliveries = 0;

    @Column(name = "on_time_delivery_rate", precision = 5, scale = 2)
    private BigDecimal onTimeDeliveryRate;

    @Column(name = "quality_pass_rate", precision = 5, scale = 2)
    private BigDecimal qualityPassRate;

    @Column(name = "average_quality_rating", precision = 3, scale = 2)
    private BigDecimal averageQualityRating;

    @Column(name = "quality_issues_count")
    @Builder.Default
    private Integer qualityIssuesCount = 0;

    @Column(name = "order_accuracy_rate", precision = 5, scale = 2)
    private BigDecimal orderAccuracyRate;

    @Column(name = "average_lead_time_days", precision = 8, scale = 2)
    private BigDecimal averageLeadTimeDays;

    @Column(name = "total_value_received", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalValueReceived = BigDecimal.ZERO;

    @Column(name = "communication_score", precision = 3, scale = 2)
    private BigDecimal communicationScore;

    @Column(name = "overall_performance_score", precision = 3, scale = 2)
    private BigDecimal overallPerformanceScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
