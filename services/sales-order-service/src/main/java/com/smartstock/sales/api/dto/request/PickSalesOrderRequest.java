package com.smartstock.sales.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PickSalesOrderRequest {

    @NotEmpty(message = "At least one pick item is required")
    @Valid
    private List<PickItemRequest> items;

    private String pickedBy;

    @Data
    public static class PickItemRequest {

        @NotBlank(message = "Line ID is required")
        private String lineId;

        @NotNull @Min(1)
        private Integer quantity;

        private String binId;
        private String location;
    }
}
