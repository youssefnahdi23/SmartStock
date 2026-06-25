package com.smartstock.inventory.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BeginCountRequest {

    @NotBlank(message = "warehouseId is required")
    private String warehouseId;

    @NotBlank(message = "countType is required")
    private String countType;

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "countDate is required")
    private LocalDate countDate;

    private String countReason;
    private String expectedDuration;
    private List<String> countTeam;
}
