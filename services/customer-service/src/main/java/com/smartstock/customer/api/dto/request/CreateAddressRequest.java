package com.smartstock.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAddressRequest {

    private String label;

    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    private String stateProvince;
    private String postalCode;

    @Size(max = 2, message = "Country code must be 2 characters")
    private String countryCode;

    private String addressType;
    private Boolean isDefault;
}
