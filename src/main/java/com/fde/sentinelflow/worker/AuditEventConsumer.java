package com.fde.sentinelflow.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    @KafkaListener(topics = "raw-audit-events", groupId = "sentinelflow-workers")
    public void consumeEvent(String message) {
        log.info("-> [Kafka Consumer Worker] Processed incoming event payload: {}", message);
    }
}
