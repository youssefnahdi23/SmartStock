package com.smartstock.identity.repository;

import com.smartstock.identity.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    Optional<Permission> findByResourceAndActionAndScope(String resource, String action, String scope);
}
