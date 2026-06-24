package com.smartstock.identity.domain.repository;

import com.smartstock.identity.domain.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    Optional<Permission> findByName(String name);

    @Query("SELECT p FROM Permission p WHERE p.name = :name AND p.active = true")
    Optional<Permission> findByNameAndActive(@Param("name") String name);

    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.action = :action AND p.active = true")
    Optional<Permission> findByResourceAndActionAndActive(@Param("resource") String resource,
                                                          @Param("action") String action);

    boolean existsByName(String name);

    List<Permission> findAllByActiveTrue();
}
