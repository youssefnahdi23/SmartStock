package com.smartstock.supplier.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SuspendSupplierRequest {

    @NotBlank(message = "Suspension reason is required")
    private String reason;

    private LocalDate resumeDate;

    private String notes;
}
