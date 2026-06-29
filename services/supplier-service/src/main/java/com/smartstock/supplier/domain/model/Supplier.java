package com.smartstock.supplier.domain.model;

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
@Table(name = "suppliers",
        uniqueConstraints = @UniqueConstraint(name = "uq_supplier_code", columnNames = {"supplier_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "supplier_code", nullable = false, length = 100)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 255)
    private String supplierName;

    @Column(name = "supplier_type", nullable = false, length = 50)
    @Builder.Default
    private String supplierType = "VENDOR";

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

    @Column(name = "currency_code", length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Column(name = "country_code", nullable = false, length = 2)
    @Builder.Default
    private String countryCode = "US";

    @Column(name = "headquarter_address")
    private String headquarterAddress;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_province", length = 100)
    private String stateProvince;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "primary_contact_id", length = 36)
    private String primaryContactId;

    @Column(name = "account_manager_id", length = 36)
    private String accountManagerId;

    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "average_lead_time_days")
    @Builder.Default
    private Integer averageLeadTimeDays = 7;

    @Column(name = "minimum_order_quantity")
    @Builder.Default
    private Integer minimumOrderQuantity = 1;

    @Column(name = "minimum_order_value", precision = 12, scale = 2)
    private BigDecimal minimumOrderValue;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "verification_date")
    private Instant verificationDate;

    @Column(name = "risk_rating", length = 50)
    @Builder.Default
    private String riskRating = "MEDIUM";

    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "total_orders")
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "total_spent", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "suspension_reason")
    private String suspensionReason;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "resume_date")
    private LocalDate resumeDate;

    @Column(name = "notes")
    private String notes;

    @Column(name = "certifications")
    private String certifications;

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

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SupplierContact> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SupplierContract> contracts = new ArrayList<>();

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    public boolean isSuspended() {
        return !Boolean.TRUE.equals(isActive) && suspensionReason != null;
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

    public void incrementOrderCount(BigDecimal orderValue) {
        this.totalOrders = (this.totalOrders == null ? 0 : this.totalOrders) + 1;
        this.totalSpent = (this.totalSpent == null ? BigDecimal.ZERO : this.totalSpent).add(orderValue);
    }
}
