package com.smartstock.warehouse.api.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneResponse {

    private String id;
    private String warehouseId;
    private String code;
    private String name;
    private String description;
    private String type;

    private BigDecimal floorSpace;
    private Integer maxCapacity;
    private Integer usedCapacity;
    private Double occupancyPercentage;

    private TemperatureData temperature;

    private int shelfCount;
    private int totalBins;

    private boolean active;
    private String createdAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TemperatureData {
        private BigDecimal min;
        private BigDecimal max;
        private String unit;
    }
}
