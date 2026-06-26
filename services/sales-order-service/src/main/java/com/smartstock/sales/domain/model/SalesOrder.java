package com.smartstock.sales.domain.model;

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
@Table(name = "sales_orders",
        uniqueConstraints = @UniqueConstraint(name = "uq_so_number", columnNames = {"so_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "so_number", nullable = false, length = 100)
    private String soNumber;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "picking_warehouse_id", length = 36)
    private String pickingWarehouseId;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "CREATED";

    @Column(name = "fulfillment_status", length = 50)
    @Builder.Default
    private String fulfillmentStatus = "PENDING";

    @Column(name = "payment_status", length = 50)
    @Builder.Default
    private String paymentStatus = "UNPAID";

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(name = "shipping_method", length = 50)
    private String shippingMethod;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "total_quantity")
    @Builder.Default
    private Integer totalQuantity = 0;

    @Column(name = "picked_quantity")
    @Builder.Default
    private Integer pickedQuantity = 0;

    @Column(name = "shipped_quantity")
    @Builder.Default
    private Integer shippedQuantity = 0;

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

    @Column(name = "confirmation_date")
    private Instant confirmationDate;

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

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SOLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderShipment> shipments = new ArrayList<>();

    public boolean canBeConfirmed() {
        return "CREATED".equals(status);
    }

    public boolean canBeCancelled() {
        return "CREATED".equals(status) || "CONFIRMED".equals(status);
    }

    public boolean canBePicked() {
        return "CONFIRMED".equals(status) || "PICKING".equals(status);
    }

    public boolean canBeShipped() {
        return "CONFIRMED".equals(status) || "PICKING".equals(status);
    }

    public void confirm(String warehouseId, String actorId) {
        this.status = "CONFIRMED";
        this.fulfillmentStatus = "PENDING";
        this.confirmationDate = Instant.now();
        this.pickingWarehouseId = warehouseId;
        this.updatedBy = actorId;
    }

    public void cancel(String reason, String actorId) {
        this.status = "CANCELLED";
        this.cancellationReason = reason;
        this.cancelledAt = Instant.now();
        this.updatedBy = actorId;
    }

    public void recalculateTotals() {
        this.totalQuantity = lineItems.stream().mapToInt(SOLineItem::getQuantityOrdered).sum();
        this.totalLineAmount = lineItems.stream()
                .map(SOLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = totalLineAmount.subtract(discountAmount).add(taxAmount);
    }
}
