package com.smartstock.product.domain.repository;

import com.smartstock.product.domain.model.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, String> {

    List<ProductSku> findByProductIdAndActiveTrue(String productId);

    Optional<ProductSku> findBySkuValue(String skuValue);

    boolean existsBySkuValue(String skuValue);
}
