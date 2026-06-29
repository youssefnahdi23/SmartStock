package com.smartstock.inventory.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "stock_out")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockOut {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // Optimistic lock (debt C-3) on this mutable dispatch aggregate.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "stock_movement_id", nullable = false, length = 36)
    private String stockMovementId;

    @Column(name = "order_id", length = 36)
    private String orderId;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "shipped_by", length = 36)
    private String shippedBy;

    @Column(name = "delivery_date")
    private Instant deliveryDate;

    @Column(name = "destination_address", columnDefinition = "TEXT")
    private String destinationAddress;

    @Column(name = "tracking_number", length = 255)
    private String trackingNumber;

    @Column(name = "is_back_order")
    @Builder.Default
    private Boolean isBackOrder = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
