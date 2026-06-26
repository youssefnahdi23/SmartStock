package com.smartstock.purchase.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RegisterDeliveryRequest {

    @NotNull(message = "Delivery date is required")
    private LocalDate deliveryDate;

    private String carrierName;
    private String trackingNumber;

    @NotEmpty(message = "At least one delivery item is required")
    @Valid
    private List<DeliveryItemRequest> items;

    private String deliveryNotes;

    @Data
    public static class DeliveryItemRequest {

        @NotBlank(message = "Line ID is required")
        private String lineId;

        @NotNull(message = "Received quantity is required")
        @Min(value = 1, message = "Received quantity must be at least 1")
        private Integer receivedQuantity;

        @Min(value = 0, message = "Damage count must be non-negative")
        private Integer damageCount;

        private String condition;
    }
}
