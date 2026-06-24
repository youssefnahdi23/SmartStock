package com.smartstock.identity.domain.repository;

import com.smartstock.identity.domain.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    Optional<Role> findByName(String name);

    @Query("SELECT r FROM Role r WHERE r.name = :name AND r.active = true")
    Optional<Role> findByNameAndActive(@Param("name") String name);

    @Query("SELECT r FROM Role r WHERE r.id = :id AND r.active = true")
    Optional<Role> findByIdAndActive(@Param("id") String id);

    boolean existsByName(String name);

    List<Role> findAllByActiveTrue();

    Page<Role> findAllByActiveTrue(Pageable pageable);
}
