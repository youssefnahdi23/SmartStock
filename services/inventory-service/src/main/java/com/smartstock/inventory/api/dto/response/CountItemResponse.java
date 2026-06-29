package com.smartstock.inventory.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CountItemResponse {

    private String countItemId;
    private String countId;
    private String productId;
    private String productName;
    private String productSku;
    private Integer systemQuantity;
    private Integer countedQuantity;
    private Integer variance;
    private Double variancePercentage;
    private String location;
    private String condition;
    private String recordedBy;
    private Instant timestamp;
    private String notes;
}
