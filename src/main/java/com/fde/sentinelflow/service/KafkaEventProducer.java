package com.fde.sentinelflow.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class KafkaEventProducer {

    private static final String TOPIC = "raw-audit-events";
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public KafkaEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper,
                              MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
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

            String jsonMessage = objectMapper.writeValueAsString(message);

            // Publish to Kafka
            kafkaTemplate.send(TOPIC, tenantId, jsonMessage);

            // Increment custom Micrometer counter with tags for analytics
            Counter.builder("sentinelflow.events.published")
                    .tag("tenant.id", tenantId)
                    .tag("event.type", eventType)
                    .description("Total number of audit events successfully published to Kafka")
                    .register(meterRegistry)
                    .increment();

        } catch (Exception e) {
            // Increment failure counter
            Counter.builder("sentinelflow.events.failed")
                    .tag("tenant.id", tenantId)
                    .description("Total number of audit events that failed publishing")
                    .register(meterRegistry)
                    .increment();

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