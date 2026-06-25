package com.smartstock.supplier.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "supplier_deliveries",
        uniqueConstraints = @UniqueConstraint(name = "uq_delivery_number", columnNames = {"delivery_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;

    @Column(name = "purchase_order_id", length = 36)
    private String purchaseOrderId;

    @Column(name = "delivery_number", nullable = false, length = 100)
    private String deliveryNumber;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "promised_delivery_date", nullable = false)
    private LocalDate promisedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Column(name = "quantity_ordered", nullable = false)
    private Integer quantityOrdered;

    @Column(name = "quantity_received", nullable = false)
    @Builder.Default
    private Integer quantityReceived = 0;

    @Column(name = "quantity_rejected")
    @Builder.Default
    private Integer quantityRejected = 0;

    @Column(name = "delivery_status", nullable = false, length = 50)
    @Builder.Default
    private String deliveryStatus = "PENDING";

    @Column(name = "on_time")
    private Boolean onTime;

    @Column(name = "on_time_days_variance")
    private Integer onTimeDaysVariance;

    @Column(name = "quality_inspection_status", length = 50)
    @Builder.Default
    private String qualityInspectionStatus = "PENDING";

    @Column(name = "quality_issues_found")
    @Builder.Default
    private Integer qualityIssuesFound = 0;

    @Column(name = "quality_rating", precision = 3, scale = 2)
    private BigDecimal qualityRating;

    @Column(name = "carrier_name", length = 255)
    private String carrierName;

    @Column(name = "tracking_number", length = 255)
    private String trackingNumber;

    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "notes")
    private String notes;

    @Column(name = "received_by", length = 36)
    private String receivedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void confirmDelivery(LocalDate actualDate, int quantityReceived, int quantityRejected, String receivedBy) {
        this.actualDeliveryDate = actualDate;
        this.quantityReceived = quantityReceived;
        this.quantityRejected = quantityRejected;
        this.deliveryStatus = "DELIVERED";
        this.receivedBy = receivedBy;

        if (promisedDeliveryDate != null) {
            long variance = actualDate.toEpochDay() - promisedDeliveryDate.toEpochDay();
            this.onTimeDaysVariance = (int) variance;
            this.onTime = variance <= 0;
        }
    }
}
