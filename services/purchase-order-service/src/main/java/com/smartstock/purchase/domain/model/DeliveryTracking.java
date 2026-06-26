package com.smartstock.purchase.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "delivery_tracking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "purchase_order_id", nullable = false, length = 36)
    private String purchaseOrderId;

    @Column(name = "tracking_number", length = 255)
    private String trackingNumber;

    @Column(name = "carrier_name", length = 255)
    private String carrierName;

    @Column(name = "estimated_arrival")
    private LocalDate estimatedArrival;

    @Column(name = "actual_arrival")
    private LocalDate actualArrival;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "total_received_quantity")
    @Builder.Default
    private Integer totalReceivedQuantity = 0;

    @Column(name = "damage_count")
    @Builder.Default
    private Integer damageCount = 0;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "delivery_notes")
    private String deliveryNotes;

    @Column(name = "received_by", length = 36)
    private String receivedBy;

    @Column(name = "received_at")
    private Instant receivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
