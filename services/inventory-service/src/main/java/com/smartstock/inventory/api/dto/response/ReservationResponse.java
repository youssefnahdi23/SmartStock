package com.smartstock.inventory.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class ReservationResponse {

    private String reservationId;
    private String productId;
    private String warehouseId;
    private Integer quantity;
    private String orderId;
    private String reservationReason;
    private String status;
    private Instant createdAt;
    private LocalDate expiryDate;
    private Integer availableStockAfterReservation;
}
