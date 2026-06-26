package com.smartstock.purchase.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class QualityIssueResponse {
    private String issueId;
    private String poId;
    private String lineId;
    private String issueType;
    private Integer quantity;
    private String description;
    private String severity;
    private String proposedResolution;
    private String status;
    private String resolutionNotes;
    private Instant resolvedAt;
    private String createdBy;
    private Instant createdAt;
}
