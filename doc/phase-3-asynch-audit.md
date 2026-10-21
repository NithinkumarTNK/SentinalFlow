Phase 3: Asynchronous Audit Log Persistence & Worker Processing
1. Overview & Objectives
   Phase 3 completes the core backend data pipeline for SentinelFlow. Having successfully established edge authentication (Phase 1) and asynchronous event streaming via Apache Kafka (Phase 2), Phase 3 introduces the downstream persistence worker layer. This layer reliably consumes events from Kafka and commits them to a high-throughput relational audit store in PostgreSQL.

Core Architecture Pillars Built:
Asynchronous Consumer Workers: Robust background event consumption powered by Spring Kafka @KafkaListener.

Reliable Deserialization & Persistence: Translating JSON audit payloads into JPA entities and committing them asynchronously to PostgreSQL.

Resilient Integration Testing: Utilizing Awaitility to handle asynchronous thread timing constraints cleanly in integration test suites.

Consumer Offset Management: Configuring auto.offset.reset=earliest and dynamic group isolation for deterministic test execution.

2. Database Schema Design (schema.sql)
   The relational persistence layer is optimized for tenant-scoped querying and efficient time-series lookup indexing:

SQL
CREATE TABLE audit_logs (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
event_type VARCHAR(100) NOT NULL,
source_ip VARCHAR(45) NOT NULL,
payload TEXT,
created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Essential indices for per-tenant filtering and reporting
CREATE INDEX idx_audit_logs_tenant_id ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
3. Core Implementation Components
   A. The Audit Log JPA Entity (AuditLog.java)
   Maps the incoming audit streams to our PostgreSQL transactional store:

Java
package com.sentinelflow.worker.model;

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

    public AuditLog() {}

    public AuditLog(UUID tenantId, String eventType, String sourceIp, String payload, OffsetDateTime createdAt) {
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.sourceIp = sourceIp;
        this.payload = payload;
        this.createdAt = createdAt;
    }
}
B. The Persistence Worker Consumer (AuditEventConsumer.java)
Listens to the Kafka broker, deserializes the JSON payload using Jackson, and commits the record via Spring Data JPA:

Java
package com.sentinelflow.worker.consumer;

import com.sentinelflow.worker.model.AuditLog;
import com.sentinelflow.worker.repository.AuditLogRepository;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "raw-audit-events", 
        groupId = "sentinelflow-workers",
        properties = {"auto.offset.reset=earliest"}
    )
    public void consumeEvent(String jsonMessage) {
        try {
            IncomingAuditMessage event = objectMapper.readValue(jsonMessage, IncomingAuditMessage.class);

            AuditLog auditLog = new AuditLog(
                    UUID.fromString(event.tenantId()),
                    event.eventType(),
                    event.sourceIp(),
                    event.payload(),
                    event.timestamp()
            );

            auditLogRepository.save(auditLog);
            log.info("-> [Persistence Worker] Successfully saved audit event for tenant: {}", event.tenantId());

        } catch (Exception e) {
            log.error("-> [Persistence Worker] Failed to process and save audit event", e);
        }
    }
}

record IncomingAuditMessage(
String tenantId,
String eventType,
String sourceIp,
String payload,
OffsetDateTime timestamp
) {}
4. Verification & Testing
   Using Spring Boot MockMvc combined with Awaitility, integration tests asynchronously verify that requests hitting the edge gateway successfully propagate through Kafka and land inside the PostgreSQL audit_logs table.