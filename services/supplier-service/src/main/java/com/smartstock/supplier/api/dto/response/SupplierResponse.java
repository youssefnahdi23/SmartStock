package com.smartstock.supplier.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class SupplierResponse {

    private String id;
    private String supplierCode;
    private String supplierName;
    private String supplierType;
    private String status;
    private String businessRegistrationNumber;
    private String taxId;
    private String websiteUrl;
    private String emailAddress;
    private String phoneNumber;
    private String paymentTerms;
    private String currencyCode;
    private String countryCode;
    private String headquarterAddress;
    private String city;
    private String stateProvince;
    private String postalCode;
    private BigDecimal creditLimit;
    private Integer averageLeadTimeDays;
    private Integer minimumOrderQuantity;
    private BigDecimal minimumOrderValue;
    private Boolean isVerified;
    private String riskRating;
    private BigDecimal rating;
    private Integer totalOrders;
    private BigDecimal totalSpent;
    private String certifications;
    private String notes;
    private String suspensionReason;
    private Instant suspendedAt;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
