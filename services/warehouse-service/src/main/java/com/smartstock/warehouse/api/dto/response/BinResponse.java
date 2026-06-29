package com.smartstock.warehouse.api.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BinResponse {

    private String id;
    private String shelfId;
    private String code;
    private String name;
    private Integer position;
    private Integer capacity;
    private Integer usedCapacity;
    private int availableCapacity;
    private String type;
    private boolean active;
    private boolean full;
    private String currentProductId;
    private BigDecimal maxWeightKg;
    private String createdAt;
}
