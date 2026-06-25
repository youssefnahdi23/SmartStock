package com.smartstock.product.unit;

import com.smartstock.product.api.dto.request.CreateCategoryRequest;
import com.smartstock.product.api.dto.response.CategoryResponse;
import com.smartstock.product.domain.model.Category;
import com.smartstock.product.domain.repository.CategoryRepository;
import com.smartstock.product.exception.BusinessException;
import com.smartstock.product.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService unit tests")
class CategoryServiceUnitTest {

    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private static final String USER_ID = "user-001";

    @Test
    @DisplayName("createCategory — happy path creates root category")
    void createCategory_rootCategory_success() {
        CreateCategoryRequest req = CreateCategoryRequest.builder()
                .name("Electronics")
                .description("Electronic products")
                .build();

        when(categoryRepository.existsByNameAndActive("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId("cat-001");
            return c;
        });

        CategoryResponse response = categoryService.createCategory(req, USER_ID);

        assertThat(response.getName()).isEqualTo("Electronics");
        assertThat(response.getCategoryLevel()).isEqualTo(0);
        assertThat(response.getParentCategoryId()).isNull();
    }

    @Test
    @DisplayName("createCategory — duplicate name throws BusinessException")
    void createCategory_duplicateName_throwsException() {
        CreateCategoryRequest req = CreateCategoryRequest.builder().name("Dupe").build();
        when(categoryRepository.existsByNameAndActive("Dupe")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(req, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dupe");
    }

    @Test
    @DisplayName("getCategory — not found throws CategoryNotFoundException")
    void getCategory_notFound_throwsException() {
        when(categoryRepository.findByIdAndActive("missing")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory("missing"))
                .hasMessageContaining("missing");
    }
}
