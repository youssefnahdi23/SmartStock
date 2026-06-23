package com.smartstock.identity.application.dto;

import java.util.List;

public record PagedRoleResponse(
        List<RoleDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
