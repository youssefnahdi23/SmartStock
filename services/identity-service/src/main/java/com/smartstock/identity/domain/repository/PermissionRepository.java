package com.smartstock.identity.domain.repository;

import com.smartstock.identity.domain.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Permission repository - Data access for Permission entity.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    @Query("SELECT p FROM Permission p WHERE p.code = :code AND p.active = true")
    Optional<Permission> findByCodeAndActive(@Param("code") String code);

    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.action = :action AND p.active = true")
    Optional<Permission> findByResourceAndActionAndActive(@Param("resource") String resource,
                                                          @Param("action") String action);
}
