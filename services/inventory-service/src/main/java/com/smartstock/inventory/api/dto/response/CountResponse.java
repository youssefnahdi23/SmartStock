package com.smartstock.inventory.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CountResponse {

    private String countId;
    private String warehouseId;
    private String warehouseName;
    private String countType;
    private String name;
    private LocalDate countDate;
    private String countReason;
    private String status;
    private String expectedDuration;
    private List<String> countTeam;
    private Integer totalItemsCounted;
    private Integer totalVariances;
    private String createdBy;
    private Instant startedAt;
}
