package com.fde.sentinelflow.repository;

import com.fde.sentinelflow.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}