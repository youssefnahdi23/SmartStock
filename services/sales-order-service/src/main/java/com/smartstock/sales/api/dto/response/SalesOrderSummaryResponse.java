package com.smartstock.sales.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class SalesOrderSummaryResponse {
    private String soId;
    private String soNumber;
    private String customerId;
    private String customerName;
    private String status;
    private String fulfillmentStatus;
    private String paymentStatus;
    private LocalDate orderDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private Instant createdAt;
}
