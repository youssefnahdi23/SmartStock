package com.smartstock.identity.domain.repository;

import com.smartstock.identity.domain.model.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByPermissionKeyIgnoreCase(String permissionKey);

    List<Permission> findByIdIn(Collection<UUID> ids);

    Page<Permission> findAllByActiveTrue(Pageable pageable);
}
