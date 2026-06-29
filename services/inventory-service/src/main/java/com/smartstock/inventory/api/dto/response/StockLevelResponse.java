package com.smartstock.inventory.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class StockLevelResponse {

    private String productId;
    private String productName;
    private String productSku;
    private String warehouseId;
    private String warehouseName;
    private Integer currentStockLevel;
    private Integer reservedStock;
    private Integer availableStock;
    private Integer reorderPoint;
    private Integer reorderQuantity;
    private Integer maxStock;
    private BigDecimal unitPrice;
    private BigDecimal stockValue;
    private Boolean lowStock;
    private Instant lastMovementAt;
    private String lastMovementType;
    private Double turnoverRate;
    private Double daysOnHand;
}
