package com.smartstock.product.api.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PagedResponse<T> {

    private List<T> data;
    private Meta meta;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Meta {
        private String timestamp;
        private int page;
        private int size;
        private long total;
        private int totalPages;
        private String traceId;
    }

    public static <T> PagedResponse<T> of(Page<?> page, List<T> content) {
        return PagedResponse.<T>builder()
                .data(content)
                .meta(Meta.builder()
                        .timestamp(Instant.now().toString())
                        .page(page.getNumber())
                        .size(page.getSize())
                        .total(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }
}
