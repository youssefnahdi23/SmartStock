package com.smartstock.supplier.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ContactResponse {

    private String id;
    private String supplierId;
    private String contactName;
    private String contactTitle;
    private String emailAddress;
    private String phoneNumber;
    private String mobileNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateProvince;
    private String postalCode;
    private String contactType;
    private Boolean isPrimary;
    private Boolean isActive;
    private Instant lastContactedAt;
    private Instant createdAt;
}
