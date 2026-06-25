package com.smartstock.product.service;

import com.smartstock.product.api.dto.request.CreateCategoryRequest;
import com.smartstock.product.api.dto.response.CategoryResponse;
import com.smartstock.product.api.dto.response.PagedResponse;
import com.smartstock.product.domain.model.Category;
import com.smartstock.product.domain.repository.CategoryRepository;
import com.smartstock.product.exception.CategoryHierarchyException;
import com.smartstock.product.exception.CategoryNameExistsException;
import com.smartstock.product.exception.CategoryNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request, String userId) {
        if (categoryRepository.existsByNameAndActive(request.getName())) {
            throw new CategoryNameExistsException(request.getName());
        }

        Category parent = null;
        int level = 0;
        if (request.getParentCategoryId() != null) {
            parent = categoryRepository.findByIdAndActive(request.getParentCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.getParentCategoryId()));
            level = parent.getCategoryLevel() + 1;
            if (level > 5) {
                throw new CategoryHierarchyException();
            }
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .parentCategory(parent)
                .categoryLevel(level)
                .sortOrder(request.getSortOrder())
                .icon(request.getIcon())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved, 0L);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CategoryResponse> listCategories(String parentCategoryId, Pageable pageable) {
        Page<Category> page = categoryRepository.findAllByParent(parentCategoryId, pageable);
        return PagedResponse.of(page, page.getContent().stream()
                .map(c -> toResponse(c, categoryRepository.countProductsByCategory(c.getId())))
                .toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(String categoryId) {
        Category category = categoryRepository.findByIdAndActive(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        return toResponse(category, categoryRepository.countProductsByCategory(categoryId));
    }

    private CategoryResponse toResponse(Category category, long productCount) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentCategoryId(category.getParentCategory() != null
                        ? category.getParentCategory().getId() : null)
                .parentCategoryName(category.getParentCategory() != null
                        ? category.getParentCategory().getName() : null)
                .categoryLevel(category.getCategoryLevel())
                .sortOrder(category.getSortOrder())
                .icon(category.getIcon())
                .productCount(productCount)
                .active(category.isActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
