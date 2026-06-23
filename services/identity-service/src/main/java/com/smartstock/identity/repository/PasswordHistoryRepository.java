package com.smartstock.identity.repository;

import com.smartstock.identity.entity.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {
    List<PasswordHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
