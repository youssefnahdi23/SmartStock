package com.smartstock.sales.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateShipmentRequest {

    @NotEmpty(message = "At least one shipment item is required")
    @Valid
    private List<ShipmentItemRequest> items;

    @NotBlank(message = "Carrier name is required")
    private String carrierName;

    private String trackingNumber;

    private LocalDate estimatedDeliveryDate;

    private String shippingMethod;

    private String notes;

    @Data
    public static class ShipmentItemRequest {

        @NotBlank(message = "Line ID is required")
        private String lineId;

        @NotNull @Min(1)
        private Integer quantity;
    }
}
