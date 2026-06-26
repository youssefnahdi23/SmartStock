package com.smartstock.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SuspendCustomerRequest {

    @NotBlank(message = "Suspension reason is required")
    private String reason;

    private LocalDate resumeDate;
    private String notes;
}
