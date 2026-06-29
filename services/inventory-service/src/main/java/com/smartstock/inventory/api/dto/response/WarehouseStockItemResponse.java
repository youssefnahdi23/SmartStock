package com.smartstock.inventory.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class WarehouseStockItemResponse {

    private String productId;
    private String productName;
    private String productSku;
    private Integer stockLevel;
    private Integer reservedStock;
    private Integer availableStock;
    private Integer reorderPoint;
    private BigDecimal stockValue;
    private Boolean lowStock;
    private Instant lastMovementAt;
}
