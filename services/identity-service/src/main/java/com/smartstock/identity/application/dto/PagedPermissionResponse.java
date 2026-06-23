package com.smartstock.identity.application.dto;

import java.util.List;

public record PagedPermissionResponse(
        List<PermissionDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
