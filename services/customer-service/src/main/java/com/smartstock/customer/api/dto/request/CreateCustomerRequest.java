package com.smartstock.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCustomerRequest {

    @NotBlank(message = "Customer code is required")
    @Size(max = 100, message = "Customer code must not exceed 100 characters")
    private String customerCode;

    @NotBlank(message = "Customer name is required")
    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    private String customerName;

    private String customerType;
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
    private String segment;
    private String riskRating;
    private String accountManagerId;
    private String notes;
}
