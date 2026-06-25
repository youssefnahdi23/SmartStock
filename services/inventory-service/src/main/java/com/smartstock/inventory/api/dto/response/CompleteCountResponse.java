package com.smartstock.inventory.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CompleteCountResponse {

    private String countId;
    private String warehouseId;
    private String status;
    private Integer totalItemsCounted;
    private Integer totalVariances;
    private Double varianceRate;
    private Integer adjustmentsCreated;
    private String completedBy;
    private Instant completedAt;
    private String approverComments;
}
