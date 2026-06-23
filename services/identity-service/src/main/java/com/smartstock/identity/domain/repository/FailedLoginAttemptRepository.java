package com.smartstock.identity.domain.repository;

import com.smartstock.identity.domain.model.FailedLoginAttempt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedLoginAttemptRepository extends JpaRepository<FailedLoginAttempt, UUID> {

    Optional<FailedLoginAttempt> findByUsernameIgnoreCase(String username);
}
