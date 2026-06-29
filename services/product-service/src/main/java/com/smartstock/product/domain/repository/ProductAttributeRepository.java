package com.smartstock.product.domain.repository;

import com.smartstock.product.domain.model.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, String> {

    List<ProductAttribute> findByProductIdOrderBySortOrder(String productId);

    @Modifying
    @Query("DELETE FROM ProductAttribute a WHERE a.product.id = :productId")
    void deleteAllByProductId(@Param("productId") String productId);
}
