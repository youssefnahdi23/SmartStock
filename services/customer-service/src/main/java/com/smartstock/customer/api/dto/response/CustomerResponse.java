package com.smartstock.customer.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private String id;
    private String customerCode;
    private String customerName;
    private String customerType;
    private String status;
    private String segment;
    private String companyName;
    private String industry;
    private String businessRegistrationNumber;
    private String taxId;
    private String websiteUrl;
    private String emailAddress;
    private String phoneNumber;
    private String paymentTerms;
    private String preferredCurrency;
    private BigDecimal creditLimit;
    private BigDecimal currentCreditBalance;
    private BigDecimal creditAvailable;
    private Integer totalOrders;
    private BigDecimal totalSpent;
    private BigDecimal averageOrderValue;
    private BigDecimal lifetimeValue;
    private BigDecimal customerRating;
    private LocalDate firstOrderDate;
    private LocalDate lastOrderDate;
    private String riskRating;
    private String accountManagerId;
    private Boolean isVerified;
    private String suspensionReason;
    private Instant suspendedAt;
    private LocalDate resumeDate;
    private String notes;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
