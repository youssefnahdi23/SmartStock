package com.smartstock.customer.domain.model;

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
@Table(name = "customers",
        uniqueConstraints = @UniqueConstraint(name = "uq_customer_code", columnNames = {"customer_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "customer_code", nullable = false, length = 100)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(name = "customer_type", nullable = false, length = 50)
    @Builder.Default
    private String customerType = "RETAIL";

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "business_registration_number", length = 100)
    private String businessRegistrationNumber;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "email_address", length = 255)
    private String emailAddress;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "preferred_currency", length = 3)
    @Builder.Default
    private String preferredCurrency = "USD";

    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "current_credit_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal currentCreditBalance = BigDecimal.ZERO;

    @Column(name = "total_orders")
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "total_spent", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "average_order_value", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal averageOrderValue = BigDecimal.ZERO;

    @Column(name = "lifetime_value", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal lifetimeValue = BigDecimal.ZERO;

    @Column(name = "customer_rating", precision = 3, scale = 2)
    private BigDecimal customerRating;

    @Column(name = "first_order_date")
    private LocalDate firstOrderDate;

    @Column(name = "last_order_date")
    private LocalDate lastOrderDate;

    @Column(name = "segment", length = 50)
    @Builder.Default
    private String segment = "STANDARD";

    @Column(name = "risk_rating", length = 50)
    @Builder.Default
    private String riskRating = "LOW";

    @Column(name = "account_manager_id", length = 36)
    private String accountManagerId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "suspension_reason")
    private String suspensionReason;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "resume_date")
    private LocalDate resumeDate;

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

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CustomerContact> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CustomerAddress> addresses = new ArrayList<>();

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    public boolean isSuspended() {
        return !Boolean.TRUE.equals(isActive) && suspensionReason != null;
    }

    public String resolveStatus() {
        if (Boolean.TRUE.equals(isActive)) return "ACTIVE";
        if (suspensionReason != null) return "SUSPENDED";
        return "INACTIVE";
    }

    public void activate() {
        this.isActive = true;
        this.suspensionReason = null;
        this.suspendedAt = null;
        this.resumeDate = null;
    }

    public void suspend(String reason, LocalDate resumeDate) {
        this.isActive = false;
        this.suspensionReason = reason;
        this.suspendedAt = Instant.now();
        this.resumeDate = resumeDate;
    }

    public void deactivate() {
        this.isActive = false;
        this.suspensionReason = null;
        this.suspendedAt = null;
        this.resumeDate = null;
    }

    public void recordOrder(BigDecimal orderValue) {
        this.totalOrders = (this.totalOrders == null ? 0 : this.totalOrders) + 1;
        this.totalSpent = (this.totalSpent == null ? BigDecimal.ZERO : this.totalSpent).add(orderValue);
        this.lastOrderDate = LocalDate.now();
        if (this.firstOrderDate == null) this.firstOrderDate = this.lastOrderDate;
        if (this.totalOrders > 0) {
            this.averageOrderValue = this.totalSpent.divide(BigDecimal.valueOf(this.totalOrders), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    public BigDecimal getCreditAvailable() {
        if (creditLimit == null) return BigDecimal.ZERO;
        BigDecimal used = currentCreditBalance == null ? BigDecimal.ZERO : currentCreditBalance;
        BigDecimal available = creditLimit.subtract(used);
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }
}
