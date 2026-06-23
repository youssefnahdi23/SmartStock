package com.smartstock.identity.application.dto;

import java.util.List;

public record UserPageResponse(
        List<UserCreateResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
