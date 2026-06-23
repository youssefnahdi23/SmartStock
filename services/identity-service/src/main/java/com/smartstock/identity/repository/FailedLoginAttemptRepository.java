package com.smartstock.identity.repository;

import com.smartstock.identity.entity.FailedLoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FailedLoginAttemptRepository extends JpaRepository<FailedLoginAttempt, UUID> {
    Optional<FailedLoginAttempt> findByUsername(String username);
    void deleteByUsername(String username);
}
