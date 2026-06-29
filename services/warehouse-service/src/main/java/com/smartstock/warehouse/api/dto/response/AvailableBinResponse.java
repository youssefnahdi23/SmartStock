package com.smartstock.warehouse.api.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableBinResponse {

    private String binId;
    private String code;
    private String zoneId;
    private String zoneName;
    private String shelfId;
    private String position;
    private int availableCapacity;
    private String compatibility;
}
