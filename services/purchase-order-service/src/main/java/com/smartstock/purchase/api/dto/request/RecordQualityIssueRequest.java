package com.smartstock.purchase.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecordQualityIssueRequest {

    private String lineId;

    @NotBlank(message = "Issue type is required")
    private String issueType;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String description;

    private String severity;

    private String proposedResolution;
}
