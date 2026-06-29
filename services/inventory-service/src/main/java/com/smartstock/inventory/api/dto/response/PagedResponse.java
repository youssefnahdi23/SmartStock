package com.smartstock.inventory.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PagedResponse<T> {

    private List<T> data;
    private Meta meta;

    @Data
    @Builder
    public static class Meta {
        private Instant timestamp;
        private int page;
        private int size;
        private long total;
        private int totalPages;
        private BigDecimal totalStockValue;
        private String traceId;
    }
}
