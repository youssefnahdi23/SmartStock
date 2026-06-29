package com.smartstock.warehouse.domain.repository;

import com.smartstock.warehouse.domain.model.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, String> {

    @Query("SELECT w FROM Warehouse w WHERE w.id = :id AND w.deletedAt IS NULL")
    Optional<Warehouse> findByIdAndNotDeleted(@Param("id") String id);

    boolean existsByCodeAndDeletedAtIsNull(String code);

    @Query("""
            SELECT w FROM Warehouse w
            WHERE w.deletedAt IS NULL
            AND (:search IS NULL
                 OR LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(w.code) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:type IS NULL OR w.type = :type)
            AND (:active IS NULL OR w.active = :active)
            """)
    Page<Warehouse> findAllWithFilters(
            @Param("search") String search,
            @Param("type") String type,
            @Param("active") Boolean active,
            Pageable pageable);
}
