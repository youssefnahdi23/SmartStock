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
public class CustomerSummaryResponse {

    private String id;
    private String customerCode;
    private String customerName;
    private String customerType;
    private String status;
    private String segment;
    private String emailAddress;
    private String phoneNumber;
    private BigDecimal creditLimit;
    private BigDecimal creditAvailable;
    private Integer totalOrders;
    private BigDecimal totalSpent;
    private BigDecimal customerRating;
    private LocalDate lastOrderDate;
    private String riskRating;
    private Boolean isVerified;
    private Instant createdAt;
}
