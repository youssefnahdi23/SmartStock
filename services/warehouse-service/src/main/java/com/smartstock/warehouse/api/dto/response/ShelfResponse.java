package com.smartstock.warehouse.api.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShelfResponse {

    private String id;
    private String zoneId;
    private String code;
    private String name;
    private Integer level;
    private Integer capacity;
    private Integer usedCapacity;
    private BigDecimal weightLimit;
    private int binCount;
    private boolean active;
    private String createdAt;
}
