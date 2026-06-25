package com.smartstock.supplier.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateSupplierRequest {

    @NotBlank(message = "Supplier code is required")
    @Size(max = 100, message = "Supplier code must not exceed 100 characters")
    private String supplierCode;

    @NotBlank(message = "Supplier name is required")
    @Size(max = 255, message = "Supplier name must not exceed 255 characters")
    private String supplierName;

    @Pattern(regexp = "VENDOR|DISTRIBUTOR|MANUFACTURER|WHOLESALER|AGENT",
             message = "Type must be one of: VENDOR, DISTRIBUTOR, MANUFACTURER, WHOLESALER, AGENT")
    private String supplierType = "VENDOR";

    private String businessRegistrationNumber;

    private String taxId;

    @Size(max = 500)
    private String websiteUrl;

    @Email(message = "Invalid email address")
    private String emailAddress;

    @Size(max = 20)
    private String phoneNumber;

    private String paymentTerms;

    @Size(max = 3)
    private String currencyCode = "USD";

    @Size(max = 2)
    private String countryCode = "US";

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

    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL",
             message = "Risk rating must be one of: LOW, MEDIUM, HIGH, CRITICAL")
    private String riskRating = "MEDIUM";
}
