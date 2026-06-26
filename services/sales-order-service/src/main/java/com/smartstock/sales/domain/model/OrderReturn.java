package com.smartstock.sales.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "sales_order_id", nullable = false, length = 36)
    private String salesOrderId;

    @Column(name = "return_number", nullable = false, unique = true, length = 100)
    private String returnNumber;

    @Column(name = "return_reason", nullable = false, length = 500)
    private String returnReason;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "INITIATED";

    @Column(name = "items_returned")
    @Builder.Default
    private Integer itemsReturned = 0;

    @Column(name = "refund_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "carrier_name", length = 255)
    private String carrierName;

    @Column(name = "tracking_number", length = 255)
    private String trackingNumber;

    @Column(name = "notes")
    private String notes;

    @Column(name = "requested_by", nullable = false, length = 36)
    private String requestedBy;

    @Column(name = "approved_by", length = 36)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void approve(String actorId) {
        this.status = "APPROVED";
        this.approvedBy = actorId;
        this.approvedAt = Instant.now();
    }
}
