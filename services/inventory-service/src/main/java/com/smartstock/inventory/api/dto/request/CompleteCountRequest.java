package com.smartstock.inventory.api.dto.request;

import lombok.Data;

@Data
public class CompleteCountRequest {
    private String approverComments;
    private boolean autoAdjust = false;
}
