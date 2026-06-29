package com.smartstock.inventory.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RecordCountItemRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    @NotNull(message = "countedQuantity is required")
    @Min(value = 0, message = "countedQuantity must be non-negative")
    private Integer countedQuantity;

    private String location;
    private String condition;
    private String notes;
}
