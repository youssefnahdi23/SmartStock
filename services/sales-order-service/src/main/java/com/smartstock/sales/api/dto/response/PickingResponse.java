package com.smartstock.sales.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PickingResponse {
    private String soId;
    private String fulfillmentStatus;
    private Integer pickedQuantity;
    private Instant pickingCompletedAt;
    private List<PickedItemResult> items;

    @Data
    @Builder
    public static class PickedItemResult {
        private String lineId;
        private String productId;
        private Integer pickedQuantity;
        private String location;
    }
}
