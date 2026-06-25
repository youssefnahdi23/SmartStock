package com.smartstock.product.api.controller;

import com.smartstock.product.api.dto.request.CreateCategoryRequest;
import com.smartstock.product.api.dto.response.CategoryResponse;
import com.smartstock.product.api.dto.response.PagedResponse;
import com.smartstock.product.security.SecurityUserDetails;
import com.smartstock.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Tag(name = "Categories", description = "Product category management")
@RestController
@RequestMapping("/products/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Create a product category")
    @PostMapping
    @PreAuthorize("hasAuthority('product:category:create')")
    public ResponseEntity<Map<String, Object>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request,
            @AuthenticationPrincipal SecurityUserDetails principal) {
        CategoryResponse category = categoryService.createCategory(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("data", category, "meta", Map.of("timestamp", Instant.now().toString())));
    }

    @Operation(summary = "List product categories")
    @GetMapping
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<PagedResponse<CategoryResponse>> listCategories(
            @RequestParam(required = false) String parentCategoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sortOrder", "name"));
        return ResponseEntity.ok(categoryService.listCategories(parentCategoryId, pageable));
    }

    @Operation(summary = "Get a category by ID")
    @GetMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('product:read')")
    public ResponseEntity<Map<String, Object>> getCategory(@PathVariable String categoryId) {
        CategoryResponse category = categoryService.getCategory(categoryId);
        return ResponseEntity.ok(
                Map.of("data", category, "meta", Map.of("timestamp", Instant.now().toString())));
    }
}
