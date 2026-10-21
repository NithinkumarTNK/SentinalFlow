package com.fde.sentinelflow.worker;

import com.fde.sentinelflow.model.AuditLog;
import com.fde.sentinelflow.repository.AuditLogRepository;
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

    @KafkaListener(topics = "raw-audit-events", groupId = "sentinelflow-workers",
    properties = {"auto.offset.reset=earliest"})
    public void consumeEvent(String jsonMessage) {
        try {
            // Deserialize incoming JSON string back into record/dto
            IncomingAuditMessage event = objectMapper.readValue(jsonMessage, IncomingAuditMessage.class);

            // Map to JPA Entity
            AuditLog auditLog = new AuditLog(
                    UUID.fromString(event.tenantId()),
                    event.eventType(),
                    event.sourceIp(),
                    event.payload(),
                    event.timestamp()
            );

            // Persist asynchronously to PostgreSQL
            auditLogRepository.save(auditLog);
            log.info("-> [Persistence Worker] Successfully saved audit event for tenant: {}", event.tenantId());

        } catch (Exception e) {
            log.error("-> [Persistence Worker] Error processing event. Triggering retry/DLQ flow...", e);
            // Re-throw the exception so Spring Kafka's DefaultErrorHandler catches it
            throw new RuntimeException("Failed to process audit event", e);
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