package com.smartstock.sales.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterDeliveryRequest {

    @NotNull(message = "Delivery date is required")
    private LocalDate deliveryDate;

    private String signedBy;

    private String deliveryNotes;
}
