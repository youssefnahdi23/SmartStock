package com.smartstock.supplier.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateContactRequest {

    @NotBlank(message = "Contact name is required")
    @Size(max = 255)
    private String contactName;

    @Size(max = 100)
    private String contactTitle;

    @Email(message = "Invalid email address")
    private String emailAddress;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 20)
    private String mobileNumber;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateProvince;

    @Size(max = 20)
    private String postalCode;

    @Pattern(regexp = "GENERAL|SALES|BILLING|TECHNICAL|LOGISTICS",
             message = "Contact type must be one of: GENERAL, SALES, BILLING, TECHNICAL, LOGISTICS")
    private String contactType = "GENERAL";

    private Boolean isPrimary = false;
}
