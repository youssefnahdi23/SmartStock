package com.smartstock.customer.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {

    private String id;
    private String customerId;
    private String contactName;
    private String contactTitle;
    private String emailAddress;
    private String phoneNumber;
    private String mobileNumber;
    private String contactType;
    private Boolean isPrimary;
    private Boolean isActive;
    private String preferredContactMethod;
    private Instant lastContactedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
