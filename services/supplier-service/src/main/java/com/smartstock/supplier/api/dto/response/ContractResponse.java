package com.smartstock.supplier.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class ContractResponse {

    private String id;
    private String supplierId;
    private String supplierName;
    private String contractNumber;
    private String contractTitle;
    private String contractType;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate renewalDate;
    private BigDecimal contractValue;
    private String paymentTerms;
    private BigDecimal discountPercentage;
    private Integer minimumVolume;
    private String contractStatus;
    private String approvalStatus;
    private String approvedBy;
    private Instant approvedAt;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean expired;
}
