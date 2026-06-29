package com.smartstock.purchase.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders",
        uniqueConstraints = @UniqueConstraint(name = "uq_po_number", columnNames = {"po_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // Optimistic lock (S-5): prevents concurrent read-modify-write lost updates on PO status
    // transitions (e.g. two confirmations racing). Hibernate bumps this on every UPDATE.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "po_number", nullable = false, length = 100)
    private String poNumber;

    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;

    @Column(name = "supplier_name", length = 255)
    private String supplierName;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "delivery_warehouse_id", length = 36)
    private String deliveryWarehouseId;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "CREATED";

    @Column(name = "shipping_method", length = 50)
    private String shippingMethod;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "total_quantity")
    @Builder.Default
    private Integer totalQuantity = 0;

    @Column(name = "delivered_quantity")
    @Builder.Default
    private Integer deliveredQuantity = 0;

    @Column(name = "total_line_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalLineAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "delivery_status", length = 50)
    @Builder.Default
    private String deliveryStatus = "NOT_RECEIVED";

    @Column(name = "payment_status", length = 50)
    @Builder.Default
    private String paymentStatus = "UNPAID";

    @Column(name = "confirmation_date")
    private Instant confirmationDate;

    @Column(name = "confirmation_number", length = 100)
    private String confirmationNumber;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 36)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<POLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<QualityIssue> qualityIssues = new ArrayList<>();

    public boolean canBeConfirmed() {
        return "CREATED".equals(status);
    }

    public boolean canBeCancelled() {
        return "CREATED".equals(status) || "CONFIRMED".equals(status);
    }

    public boolean canReceiveDelivery() {
        return "CONFIRMED".equals(status) || "SHIPPED".equals(status);
    }

    public void confirm(String confirmationNumber, String actorId) {
        this.status = "CONFIRMED";
        this.confirmationDate = Instant.now();
        this.confirmationNumber = confirmationNumber;
        this.updatedBy = actorId;
    }

    public void cancel(String reason, String actorId) {
        this.status = "CANCELLED";
        this.cancellationReason = reason;
        this.cancelledAt = Instant.now();
        this.updatedBy = actorId;
    }

    public void recalculateTotals() {
        this.totalQuantity = lineItems.stream().mapToInt(POLineItem::getQuantityOrdered).sum();
        this.totalLineAmount = lineItems.stream()
                .map(POLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = totalLineAmount.subtract(discountAmount).add(taxAmount);
    }
}
