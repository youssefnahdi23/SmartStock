package com.smartstock.supplier.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateSupplierRequest {

    @Size(max = 255)
    private String supplierName;

    @Size(max = 500)
    private String websiteUrl;

    @Email
    private String emailAddress;

    @Size(max = 20)
    private String phoneNumber;

    private String paymentTerms;

    @Size(max = 3)
    private String currencyCode;

    private String headquarterAddress;
    private String city;
    private String stateProvince;

    @Size(max = 20)
    private String postalCode;

    @DecimalMin(value = "0.0")
    private BigDecimal creditLimit;

    @Min(0)
    private Integer averageLeadTimeDays;

    @Min(1)
    private Integer minimumOrderQuantity;

    @DecimalMin(value = "0.0")
    private BigDecimal minimumOrderValue;

    private List<String> certifications;

    private String notes;

    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL")
    private String riskRating;
}
