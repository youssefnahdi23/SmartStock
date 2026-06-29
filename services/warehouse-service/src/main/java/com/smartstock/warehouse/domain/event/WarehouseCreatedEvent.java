package com.smartstock.warehouse.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class WarehouseCreatedEvent extends DomainEvent {

    private String warehouseId;
    private String code;
    private String name;
    private String type;
    private String city;
    private String country;
    private String managerId;
    private String createdBy;
    private String status;

    public WarehouseCreatedEvent(String warehouseId, String code, String name,
                                  String type, String city, String country,
                                  String managerId, String createdBy) {
        super(warehouseId, "Warehouse", "warehouse-service");
        this.warehouseId = warehouseId;
        this.code = code;
        this.name = name;
        this.type = type;
        this.city = city;
        this.country = country;
        this.managerId = managerId;
        this.createdBy = createdBy;
        this.status = "ACTIVE";
    }
}
