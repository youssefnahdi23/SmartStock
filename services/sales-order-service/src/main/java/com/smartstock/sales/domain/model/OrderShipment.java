package com.smartstock.sales.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "order_shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Column(name = "shipment_number", nullable = false, unique = true, length = 100)
    private String shipmentNumber;

    @Column(name = "carrier_name", length = 255)
    private String carrierName;

    @Column(name = "tracking_number", length = 255)
    private String trackingNumber;

    @Column(name = "shipping_method", length = 100)
    private String shippingMethod;

    @Column(name = "shipped_quantity")
    @Builder.Default
    private Integer shippedQuantity = 0;

    @Column(name = "ship_date")
    private LocalDate shipDate;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "SHIPPED";

    @Column(name = "shipped_by", length = 36)
    private String shippedBy;

    @Column(name = "signed_by", length = 255)
    private String signedBy;

    @Column(name = "delivery_notes")
    private String deliveryNotes;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
