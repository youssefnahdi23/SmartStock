package com.smartstock.warehouse.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShelfRequest {

    @NotBlank(message = "Shelf code is required")
    @Size(max = 100, message = "Code must not exceed 100 characters")
    private String code;

    @NotBlank(message = "Shelf name is required")
    private String name;

    @Min(value = 1, message = "Shelf level must be at least 1")
    private Integer level;

    @Min(value = 0, message = "Capacity must be non-negative")
    private Integer capacity;

    private Double weightLimit;
}
