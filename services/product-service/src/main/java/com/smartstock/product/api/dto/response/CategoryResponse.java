package com.smartstock.product.api.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryResponse {

    private String id;
    private String name;
    private String description;
    private String parentCategoryId;
    private String parentCategoryName;
    private int categoryLevel;
    private int sortOrder;
    private String icon;
    private long productCount;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
