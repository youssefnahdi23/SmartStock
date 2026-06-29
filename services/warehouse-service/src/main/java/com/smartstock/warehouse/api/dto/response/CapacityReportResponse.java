package com.smartstock.warehouse.api.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapacityReportResponse {

    private String warehouseId;
    private String warehouseName;
    private String reportDate;

    private CapacityMetric floorSpace;
    private CapacityMetric pallets;
    private CapacityMetric weight;

    private List<ZoneSummary> zones;
    private List<Alert> alerts;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CapacityMetric {
        private double total;
        private double used;
        private double available;
        private double utilizationPercentage;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ZoneSummary {
        private String zoneId;
        private String zoneName;
        private double utilizationPercentage;
        private int shelves;
        private int bins;
        private int occupiedBins;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Alert {
        private String level;
        private String type;
        private String message;
    }
}
