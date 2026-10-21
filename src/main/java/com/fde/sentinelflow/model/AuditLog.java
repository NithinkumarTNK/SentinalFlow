package com.fde.sentinelflow.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "source_ip", nullable = false)
    private String sourceIp;

    private String payload;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // Default constructor for JPA
    public AuditLog() {}

    public AuditLog(UUID tenantId, String eventType, String sourceIp, String payload, OffsetDateTime createdAt) {
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.sourceIp = sourceIp;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getEventType() { return eventType; }
    public String getSourceIp() { return sourceIp; }
    public String getPayload() { return payload; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
