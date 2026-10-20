Phase 2: Asynchronous Event Streaming Backbone
1. Overview & Objectives
   Phase 2 transitions SentinelFlow from a synchronous request-response API gateway into a decoupled, highly resilient event-driven streaming architecture.

Core Architecture Pillars Built:
Asynchronous Decoupling: Ingestion gateways no longer block on heavy downstream processing; valid traffic is instantly queued.

Apache Kafka Integration: Real-time event streaming powered by local Zookeeper and Kafka broker containers.

Strict Tenant Ordering: Using tenantId as the Kafka message key to guarantee chronological ordering per tenant across partition streams.

JSON Serialization Pipeline: Robust payload serialization using Jackson ObjectMapper paired with Kafka string serializers for optimal cross-service compatibility.

2. Infrastructure Expansion (docker-compose.yml)
   We expanded the local container stack to include Apache Kafka and ZooKeeper:

YAML
version: '3.8'

services:
postgres:
image: postgres:15-alpine
container_name: sentinelflow_postgres
environment:
POSTGRES_DB: sentinelflow_postgres
POSTGRES_USER: postgres
POSTGRES_PASSWORD: password
ports:
- "5432:5432"
volumes:
- postgres_data:/var/lib/postgresql/data

redis:
image: redis:7-alpine
container_name: sentinelflow_redis
ports:
- "6379:6379"

zookeeper:
image: confluentinc/cp-zookeeper:7.5.0
container_name: sentinelflow_zookeeper
environment:
ZOOKEEPER_CLIENT_PORT: 2181
ZOOKEEPER_TICK_TIME: 2000

kafka:
image: confluentinc/cp-kafka:7.5.0
container_name: sentinelflow_kafka
depends_on:
- zookeeper
ports:
- "9092:9092"
environment:
KAFKA_BROKER_ID: 1
KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1

volumes:
postgres_data:
3. Configuration & Dependencies (pom.xml & application.yml)
   Dependencies Added:
   spring-kafka: Core Spring abstraction for Kafka producers and consumers.

jackson-databind: JSON serialization library for transforming records into payload strings.

Kafka Broker Properties (application.yml):
YAML
spring:
kafka:
bootstrap-servers: localhost:9092
producer:
key-serializer: org.apache.kafka.common.serialization.StringSerializer
value-serializer: org.apache.kafka.common.serialization.StringSerializer
properties:
spring.json.add.type.headers: false
4. Core Implementation Components
   A. The Event Producer (KafkaEventProducer.java)
   Wraps incoming ingestion requests into a standardized AuditEventMessage record, serializes it to a JSON string using Jackson's ObjectMapper, and publishes it to the raw-audit-events topic using the tenantId as the partitioning key.

Java
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
                    tenantId, eventType, sourceIp, payload, OffsetDateTime.now()
            );
            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(TOPIC, tenantId, jsonMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize and publish audit event to Kafka", e);
        }
    }
}
B. Downstream Consumer Worker (AuditEventConsumer.class)
Demonstrates asynchronous message consumption via @KafkaListener subscribed to the raw-audit-events topic group sentinelflow-workers.

5. Verification & Testing
   Integration test suites verifying successful event dispatch return an HTTP 202 Accepted response while cleanly pushing messages through the local Kafka broker pipeline.