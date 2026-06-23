package com.smartstock.identity.domain.repository;

import com.smartstock.identity.domain.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);
}
