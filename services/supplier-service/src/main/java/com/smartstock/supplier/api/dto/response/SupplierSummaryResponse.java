package com.smartstock.supplier.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class SupplierSummaryResponse {

    private String id;
    private String supplierCode;
    private String supplierName;
    private String supplierType;
    private String status;
    private String emailAddress;
    private String phoneNumber;
    private String countryCode;
    private String city;
    private BigDecimal rating;
    private Integer totalOrders;
    private BigDecimal totalSpent;
    private Boolean isVerified;
    private String riskRating;
    private Instant createdAt;
}
