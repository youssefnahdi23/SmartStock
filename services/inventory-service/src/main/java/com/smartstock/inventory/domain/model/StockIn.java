package com.smartstock.inventory.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_in")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "stock_movement_id", nullable = false, length = 36)
    private String stockMovementId;

    @Column(name = "supplier_id", length = 36)
    private String supplierId;

    @Column(name = "purchase_order_id", length = 36)
    private String purchaseOrderId;

    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    @Column(name = "quantity_received", nullable = false)
    private Integer quantityReceived;

    @Column(name = "quantity_accepted", nullable = false)
    private Integer quantityAccepted;

    @Column(name = "quantity_rejected")
    @Builder.Default
    private Integer quantityRejected = 0;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "received_by", nullable = false, length = 36)
    private String receivedBy;

    @Column(name = "inspection_status", length = 50)
    @Builder.Default
    private String inspectionStatus = "PENDING";

    @Column(name = "inspection_notes", columnDefinition = "TEXT")
    private String inspectionNotes;

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;

    @Column(name = "inspected_by")
    private String inspectedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
