package com.smartstock.identity.domain.repository;

import com.smartstock.identity.domain.model.PasswordHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {

    List<PasswordHistory> findTop5ByUser_IdOrderByCreatedAtDesc(UUID userId);
}
