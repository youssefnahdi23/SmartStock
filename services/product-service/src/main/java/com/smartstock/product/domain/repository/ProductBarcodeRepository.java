package com.smartstock.product.domain.repository;

import com.smartstock.product.domain.model.ProductBarcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductBarcodeRepository extends JpaRepository<ProductBarcode, String> {

    List<ProductBarcode> findByProductIdAndActiveTrue(String productId);

    Optional<ProductBarcode> findByBarcodeValue(String barcodeValue);

    boolean existsByBarcodeValue(String barcodeValue);

    @Query("SELECT b FROM ProductBarcode b WHERE b.product.id = :productId AND b.primary = true")
    Optional<ProductBarcode> findPrimaryByProductId(@Param("productId") String productId);
}
