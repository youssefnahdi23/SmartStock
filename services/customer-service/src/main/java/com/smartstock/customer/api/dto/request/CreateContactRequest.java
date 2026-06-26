package com.smartstock.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateContactRequest {

    @NotBlank(message = "Contact name is required")
    @Size(max = 255, message = "Contact name must not exceed 255 characters")
    private String contactName;

    private String contactTitle;
    private String emailAddress;
    private String phoneNumber;
    private String mobileNumber;
    private String contactType;
    private Boolean isPrimary;
    private String preferredContactMethod;
}
