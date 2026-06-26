package com.smartstock.sales.api.dto.request;

import lombok.Data;

@Data
public class ConfirmSalesOrderRequest {
    private String warehouseId;
    private String notes;
}
