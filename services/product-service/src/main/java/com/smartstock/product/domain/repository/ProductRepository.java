package com.smartstock.product.domain.repository;

import com.smartstock.product.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Product> findByIdAndNotDeleted(@Param("id") String id);

    @Query("SELECT p FROM Product p WHERE p.sku = :sku AND p.deletedAt IS NULL")
    Optional<Product> findBySkuAndNotDeleted(@Param("sku") String sku);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.sku = :sku AND p.deletedAt IS NULL")
    boolean existsBySkuAndNotDeleted(@Param("sku") String sku);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.sku = :sku AND p.id <> :excludeId AND p.deletedAt IS NULL")
    boolean existsBySkuAndNotDeletedExcluding(@Param("sku") String sku, @Param("excludeId") String excludeId);

    @Query("""
            SELECT DISTINCT p FROM Product p
            LEFT JOIN p.productCategories pc
            WHERE p.deletedAt IS NULL
            AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:active IS NULL OR p.active = :active)
            AND (:categoryId IS NULL OR pc.category.id = :categoryId)
            """)
    Page<Product> findAllWithFilters(
            @Param("search") String search,
            @Param("active") Boolean active,
            @Param("categoryId") String categoryId,
            Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            JOIN p.barcodes b
            WHERE b.barcodeValue = :barcode AND p.deletedAt IS NULL
            """)
    Optional<Product> findByBarcodeValue(@Param("barcode") String barcode);
}
