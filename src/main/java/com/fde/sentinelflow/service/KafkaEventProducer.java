package com.fde.sentinelflow.service;

import tools.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class KafkaEventProducer {

    private static final String TOPIC = "raw-audit-events";
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishEvent(String tenantId, String eventType, String sourceIp, String payload) {
        try {
            AuditEventMessage message = new AuditEventMessage(
                    tenantId,
                    eventType,
                    sourceIp,
                    payload,
                    OffsetDateTime.now()
            );

            // Serialize message object to JSON string
            String jsonMessage = objectMapper.writeValueAsString(message);

            // Using tenantId as the key ensures strict partition ordering per tenant
            kafkaTemplate.send(TOPIC, tenantId, jsonMessage);

        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize and publish audit event to Kafka", e);
        }
    }
}

record AuditEventMessage(
        String tenantId,
        String eventType,
        String sourceIp,
        String payload,
        OffsetDateTime timestamp
) {}