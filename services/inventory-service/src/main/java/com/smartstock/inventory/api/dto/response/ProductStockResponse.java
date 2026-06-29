package com.smartstock.inventory.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductStockResponse {

    private String productId;
    private String productName;
    private String productSku;
    private Integer totalStockLevel;
    private BigDecimal totalStockValue;
    private List<WarehouseStockEntry> warehouses;

    @Data
    @Builder
    public static class WarehouseStockEntry {
        private String warehouseId;
        private String warehouseName;
        private Integer stockLevel;
        private Integer reservedStock;
        private Integer availableStock;
        private Double percentage;
    }
}
