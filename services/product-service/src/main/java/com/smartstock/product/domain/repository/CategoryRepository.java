package com.smartstock.product.domain.repository;

import com.smartstock.product.domain.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.active = true")
    Optional<Category> findByIdAndActive(@Param("id") String id);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND c.active = true")
    boolean existsByNameAndActive(@Param("name") String name);

    @Query("""
            SELECT c FROM Category c
            WHERE c.active = true
            AND (:parentCategoryId IS NULL AND c.parentCategory IS NULL
                 OR c.parentCategory.id = :parentCategoryId)
            """)
    Page<Category> findAllByParent(
            @Param("parentCategoryId") String parentCategoryId,
            Pageable pageable);

    @Query("SELECT COUNT(pc) FROM ProductCategory pc WHERE pc.category.id = :categoryId")
    long countProductsByCategory(@Param("categoryId") String categoryId);
}
