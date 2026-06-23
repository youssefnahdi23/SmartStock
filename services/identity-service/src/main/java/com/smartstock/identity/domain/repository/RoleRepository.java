package com.smartstock.identity.domain.repository;

import com.smartstock.identity.domain.model.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = {"permissions"})
    List<Role> findByIdIn(Collection<UUID> ids);

    @EntityGraph(attributePaths = {"permissions"})
    @Query("select r from Role r left join fetch r.permissions where upper(r.name) in :names")
    List<Role> findByNames(@Param("names") Collection<String> names);

    Page<Role> findAllByActiveTrue(Pageable pageable);
}
