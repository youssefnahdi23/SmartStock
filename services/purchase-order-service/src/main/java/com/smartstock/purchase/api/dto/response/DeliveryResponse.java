package com.smartstock.purchase.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class DeliveryResponse {
    private String deliveryId;
    private String poId;
    private String status;
    private LocalDate deliveryDate;
    private String carrierName;
    private String trackingNumber;
    private Integer totalReceivedQuantity;
    private Integer damageCount;
    private String deliveryNotes;
    private Instant receivedAt;
    private List<DeliveryItemResult> items;

    @Data
    @Builder
    public static class DeliveryItemResult {
        private String lineId;
        private String productId;
        private Integer receivedQuantity;
        private Integer damageCount;
    }
}
