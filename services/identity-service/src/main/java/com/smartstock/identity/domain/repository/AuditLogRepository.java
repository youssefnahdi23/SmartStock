package com.smartstock.identity.domain.repository;

import com.smartstock.identity.domain.model.AuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
