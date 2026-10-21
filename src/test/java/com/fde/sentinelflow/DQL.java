package com.fde.sentinelflow;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class DeadLetterQueueIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldRoutePoisonPillToDlqAfterRetries() {
        String dlqTopic = "raw-audit-events.DLT";
        String malformedPayload = "{ invalid-json-payload }";

        // 1. Send malformed message directly to the raw topic to simulate a poison pill
        kafkaTemplate.send("raw-audit-events", "test-tenant", malformedPayload);

        // 2. Setup a consumer to listen on the DLQ topic
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(dlqTopic));

            // 3. Awaitility check to verify the message eventually lands in the DLQ after retries
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var records = consumer.poll(Duration.ofMillis(500));
                boolean foundInDlq = false;
                for (ConsumerRecord<String, String> record : records) {
                    if (malformedPayload.equals(record.value())) {
                        foundInDlq = true;
                        break;
                    }
                }
                assertThat(foundInDlq).isTrue();
            });
        }
    }
}